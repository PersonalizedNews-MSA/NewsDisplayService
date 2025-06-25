package com.mini2.newsdisplayservice.domain.service;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mini2.newsdisplayservice.common.exception.kind.NotFound;
import com.mini2.newsdisplayservice.domain.dto.NewsResponse;
import com.mini2.newsdisplayservice.domain.dto.NewsResultResponse;
import com.mini2.newsdisplayservice.domain.dto.favorite.LikeEvent;
import com.mini2.newsdisplayservice.domain.entity.News;
import com.mini2.newsdisplayservice.domain.repository.NewsRepository;
import com.mini2.newsdisplayservice.event.consumer.message.dto.favorite.FavoriteNewsInfoDto;
import com.mini2.newsdisplayservice.event.consumer.message.service.FavoriteNewsInfoService;
import com.mini2.newsdisplayservice.event.consumer.message.service.UserInterestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.Charset;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

// gateway에서 토큰 인증하고 헤더로 보냄
// ms에선 게이트웨이리퀘스트헤더유틸스 해서 가져오는 식


@Service
@RequiredArgsConstructor
@Slf4j
public class NewsService {
    private final static int TOTAL_ITEM_SIZE = 10;
    // 가중치 함수 조절을 위한 상수 (값이 클수록 시간이 지남에 따라 점수 감소가 가파름)
    private static final double DECAY_RATE = 0.00000000001;
    // 각 좋아요 이벤트가 기여할 수 있는 최대 점수
    private static final double MAX_SCORE_PER_LIKE_EVENT = 10.0;
    // 각 카테고리의 최종 합산 점수 최대치
    private static final double MAX_CATEGORY_TOTAL_SCORE = 50.0;
    private final NewsRepository newsRepository;
    private final OpenAiChatModel openAiChatModel;

    private final ObjectMapper objectMapper;
    private final FavoriteNewsInfoService favoriteNewsInfoService;
    private final UserInterestService userInterestService;

    @Value("${naver.client.id}")
    private String CLIENT_ID;
    @Value("${naver.search.key}")
    private String CLIENT_SECRET;

    public List<NewsResponse> getNews(Long userId) {
        List<News> newsList = newsRepository.findAll();
//        List<String> favorites = getUserFavorite(userId);
        if (newsList.isEmpty()) throw new NotFound("서버 내부 오류");

        return newsList.stream()
                .map(news -> {
                    boolean isFavorite = false;//favorites != null && favorites.contains(news.getLink());
                    return NewsResponse.builder()
                            .title(news.getTitle())
//                            .link(news.getLink())
//                            .thumbnail(news.getThumbnail())
//                            .description(news.getDescription())
                            .category(news.getCategory())
                            .favorite(isFavorite)
                            .build();
                })
                .toList();
    }

    public Map<String, Double> calculateCategoryScores(List<LikeEvent> likeEvents) {
        Map<String, Double> categoryScores = new HashMap<>();
        LocalDateTime now = LocalDateTime.now(); // 현재 시간

        for (LikeEvent event : likeEvents) {
            String category = event.getCategory();
            LocalDateTime likedAt = event.getCreatedAt();

            // 좋아요를 누른 시점과 현재 시점의 차이 계산 (밀리초 단위)
            long durationMillis = Duration.between(likedAt, now).toMillis();

            // 시간 가중치 계산 (지수 감소 함수: e^(-k * 경과 시간))
            double timeWeight = Math.exp(-DECAY_RATE * durationMillis);

            // 각 좋아요 이벤트의 기본 점수 (여기서는 1.0)에 시간 가중치를 곱함
            double scoreToAdd = 1.0 * timeWeight;

            // 1. 각 좋아요 이벤트가 기여할 수 있는 최대 점수 제한
            // scoreToAdd가 MAX_SCORE_PER_LIKE_EVENT를 초과하지 않도록 함 (거의 발생하지 않겠지만 안전장치)
            scoreToAdd = Math.min(scoreToAdd, MAX_SCORE_PER_LIKE_EVENT);

            // 카테고리별 현재 점수를 가져오거나 0으로 초기화
            double currentCategoryTotal = categoryScores.getOrDefault(category, 0.0);

            // 새로운 점수를 더함
            double newCategoryTotal = currentCategoryTotal + scoreToAdd;

            // 2. 카테고리별 최종 합산 점수 최대치 제한
            // MAX_CATEGORY_TOTAL_SCORE를 초과하지 않도록 함
            newCategoryTotal = Math.min(newCategoryTotal, MAX_CATEGORY_TOTAL_SCORE);

            categoryScores.put(category, newCategoryTotal);
        }
        return categoryScores;
    }

    @Transactional
    public String getKeyword(Long userId) {

        List<String> interests = userInterestService.getUserLatestInterests(userId);

        String interestStr = String.join(", ", interests);

        List<News> newsList = newsRepository.findAll();
        log.info("newsList 까지 {}", newsList.size());
        String titles =
                newsList.stream()
                .map(news -> news.getTitle() + " (" + news.getCategory() + ")")
                .collect(Collectors.joining("\n- ", "- ", ""));

        // TODO 최근에 좋아요 누른거 10개 중에서 가장 빈도높은 카테고리 보내기
        String favoriteStr = getTopTwoCategoryInterestsForGPT(userId);

        return openAiChatModel.call(
                """
                        1. 사용자의 관심사는 다음과 같습니다:
                        %s
                        2. 사용자가 좋아하는 뉴스 카테고리의 점수는 다음과 같습니다(100점 만점) :
                        %s
                        3. 오늘의 뉴스 헤드라인과 카테고리는 다음과 같습니다:
                        %s
                        
                        
                        
                        위 정보를 바탕으로, 다음 조건에 따라 네이버 검색 API에 사용할 적절한 검색어를 5개 추천해주세요:
                        
                        - 검색어는 가급적 넓은 범위의 관심사를 포괄하며, 구체적인 하위 주제도 포함하되 너무 좁거나 중복되지 않도록 해야 합니다.
                        - 각 검색어는 하나의 명사 또는 간결한 단어로 구성해주세요 (예: 'AI', '부동산', '일자리').
                        - 검색어는 사용자의 관심사와 오늘 뉴스 헤드라인에 언급된 키워드를 기반으로 도출해주세요.
                        - 사용자의 관심사와 관련된 키워드는 높은 우선순위를 갖습니다.
                        - 뉴스 헤드라인에서 의미 있는 키워드를 충분히 도출할 수 없을 경우, 사용자 관심사 기반으로 **최신 트렌드를 반영한 키워드**를 생성해주세요.
                        - 2번 항목에서 아무 것도 없다면 2번항목을 무시합니다. 그리고 2번 항목이 입력되어 있는데 언급되지 않은 카테고리는 관심이 없는 것입니다.
                        - 추천된 검색어는 공백으로 구분된 5개의 단어로만 출력해주세요 (쉼표 없이).
                        """.formatted(interestStr, favoriteStr, titles)

        );
    }

    public String getTopTwoCategoryInterestsForGPT(Long userId) {
        List<FavoriteNewsInfoDto> favorites = favoriteNewsInfoService.getTop10Favoriets(userId);

        /** TODO 각 카테고리에 대해:
         //선호 점수 = 관심사 포함 여부 (2점) + 좋아요 뉴스 수 (1점/개수)
         //최종 점수 = (좋아요 점수 * 가중치1) + (관심사 매칭 점수 * 가중치2)
         //예:
         //정치: 관심사 포함(2점) + 좋아요 3건(3점) → 총 5점
         //IT: 관심사 미포함(0점) + 좋아요 2건(2점) → 총 2점
         **/
        // 2. FavoriteNewsInfoDto 리스트를 LikeEvent 리스트로 변환
        List<LikeEvent> likeEvents = favorites.stream()
                .map(dto -> {
                    try {
                        LocalDateTime timestamp = dto.getCreatedTime();
                        return new LikeEvent(dto.getNewsId(), timestamp);
                    } catch (Exception e) {
                        log.error("FavoriteNewsInfoDto에서 LikeEvent 변환 중 오류 발생: {}", dto, e);
                        return null; // 변환 실패 시 null 반환 (아래에서 필터링)
                    }
                })
                .filter(event -> event != null) // 변환에 실패한 null 이벤트 필터링
                .collect(Collectors.toList());

        // 3. 변환된 LikeEvent 리스트로 실제 점수 계산 로직 호출
        Map<String, Double> finalScores = calculateCategoryScores(likeEvents);
        System.out.println("--- 카테고리별 최종 점수 (시간 가중치 및 최대 점수 제한 적용) ---");
        finalScores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed()) // 점수가 높은 순으로 정렬
                .forEach(entry -> System.out.printf("%s: %.2f점%n", entry.getKey(), entry.getValue()));

        List<Map.Entry<String, Double>> sortedCategories = finalScores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed()) // 점수 높은 순 정렬
                .limit(2) // 상위 2개만 선택
                .toList();

        // 상위 두 카테고리가 없는 경우 (예: 데이터 부족)
        if (sortedCategories.size() < 2) {
            // 상위 1개만 있을 경우 해당 카테고리만 두 배 점수로 반환
            if (sortedCategories.size() == 1) {
                Map.Entry<String, Double> top1 = sortedCategories.get(0);
                double doubledScore = top1.getValue() * 2;
                log.warn("사용자 {}의 카테고리 데이터가 부족하여 한 개의 관심 카테고리만 두 배 점수로 반환합니다: {}. 점수: {}", userId, top1.getKey(), doubledScore);
                return String.format("%s %.2f점", top1.getKey(), doubledScore);
            }
            log.warn("사용자 {}의 관심 카테고리 데이터가 충분하지 않아 상위 2개를 찾을 수 없습니다.", userId);
            return ""; // 상위 2개 카테고리를 찾을 수 없는 경우 빈 문자열 반환
        }

        // 정렬할 용도
        // 3. 선택된 두 카테고리의 점수를 두 배로 곱함
        Map.Entry<String, Double> top1 = sortedCategories.get(0);
        Map.Entry<String, Double> top2 = sortedCategories.get(1);

        double doubledScore1 = top1.getValue() * 2;
        double doubledScore2 = top2.getValue() * 2;
        String result = String.format("%s %.2f점, %s %.2f점",
                top1.getKey(), doubledScore1,
                top2.getKey(), doubledScore2);

        return result;

    }


    public NewsResultResponse getResponse(String keyword, Integer start, Long userId) {
        List<NewsResponse> dtos = new ArrayList<>();
        String[] keywords = keyword.split(" ");
        int perKeywordSize = TOTAL_ITEM_SIZE / keywords.length;
        List<String> favorites = getUserFavorite(userId);
        int max = 0;
        start = start != null ? start %= 1000 : start;
        outer:
        for (String k : keywords) {
            int collected = 0;
            start = (start == null) ? 1 : start;
            int display = 20;
            while (collected < perKeywordSize) {
                String response = naverSearchApi(k, display, start);

                try {
                    JsonNode root = objectMapper.readTree(response);
                    JsonNode items = root.path("items");

                    if (!items.isArray() || items.size() == 0) {
                        // 더 이상 결과가 없음 → 다음 키워드로 넘어감
                        break;
                    }

                    for (JsonNode item : items) {
                        String link = item.path("link").asText();
                        if (!link.contains("https://n.news.naver.com") &&
                                !link.contains("https://news.naver.com") &&
                                !link.contains("https://m.sports.naver.com") &&
                                !link.contains("https://m.entertain.naver.com")
                        ) {
                            continue;
                        }
                        if (dtos.stream().anyMatch(dto -> dto.getLink().equals(link))) {
                            continue;
                        }

                        String title = Jsoup.parse(item.path("title").asText()).text();
                        String description = Jsoup.parse(item.path("description").asText()).text();
                        List<String> newsInfo = getNewsInfo(link);
                        if (newsInfo == null) continue;
                        String pubDate = dateParser(Jsoup.parse(item.path("pubDate").asText()).text());
                        boolean isFavorite = favorites != null && favorites.contains(link);
                        dtos.add(NewsResponse.builder()
                                .title(title)
                                .link(link)
                                .thumbnail(newsInfo.get(0))
                                .category(newsInfo.get(1))
                                .description(description)
                                .favorite(isFavorite)
                                .pubDate(pubDate)
                                .build());

                        collected++;
                        if (collected >= perKeywordSize || dtos.size() >= TOTAL_ITEM_SIZE) {
                            break;
                        }
                    }

                    // 다음 페이지로 넘어갈 수 있도록 start 증가
                    start += display;
                    max = Math.max(start, max);
                    if (start > 1000) {
                        max = start;
                        start %= 1000;
                    }
                } catch (Exception e) {
                    log.error("[News Service] getResponse", e);
                    throw new NotFound("뉴스정보를 가져올 수 없습니다.");
                }
            }

            if (dtos.size() >= TOTAL_ITEM_SIZE) break outer;
        }

        return NewsResultResponse.builder()
                .newsList(dtos)
                .start(max)
                .build();
    }


    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void updateNewsHeadLine() {
        LocalDateTime batchTime = LocalDateTime.now(); // 배치 실행 시간 기록
        log.info("[News Headline Batch Start] time: {}", batchTime);

        List<NewsSection> sections = List.of(
                new NewsSection("https://news.naver.com/section/100", "정치"),
                new NewsSection("https://news.naver.com/section/101", "경제"),
                new NewsSection("https://news.naver.com/section/102", "사회"),
                new NewsSection("https://news.naver.com/section/103", "생활/문화"),
                new NewsSection("https://news.naver.com/section/104", "세계"),
                new NewsSection("https://news.naver.com/section/105", "IT/과학")
        );

        List<News> newNewsList = new ArrayList<>(); // 새로 수집된 뉴스만 담을 리스트

        try {
            for (NewsSection section : sections) {
                // Jsoup을 이용한 웹 크롤링
                Document doc = Jsoup.connect(section.url())
                        .timeout(5000) // 연결 타임아웃 5초 설정 (선택 사항)
                        .get();
                Elements newsLists = doc.select("ul[id^=_SECTION_HEADLINE_LIST_]");

                for (Element newsListElem : newsLists) {
                    Elements items = newsListElem.select("li");
                    for (Element item : items) {
                        Element linkElem = item.selectFirst("a.sa_text_title");
                        if (linkElem == null) continue;

                        String title = linkElem.text();

                        newNewsList.add(News.builder()
                                .title(title)
                                .category(section.name())
                                .createdTime(batchTime) // 배치 실행 시간을 생성 시간으로 기록
                                .build());
                    }
                }
            }

            newsRepository.saveAll(newNewsList);

            newsRepository.deleteByCreatedTimeBefore(batchTime);

            log.info("[News Headline Batch End] Fetched {} new news items. Deleted old news before {}.", newNewsList.size(), batchTime);

        } catch (Exception e) {
            log.error("[News Headline Batch Error] An error occurred during news update: {}", e.getMessage(), e);

            throw new NotFound("뉴스 정보를 가져오거나 갱신하는 중 오류가 발생했습니다.");
        }
    }


    public String naverSearchApi(String query, Integer display, Integer start) {
        if (display == null) display = 100;
        if (start == null) start = 1;
        URI uri = UriComponentsBuilder
                .fromUriString("https://openapi.naver.com")
                .path("/v1/search/news.json")
                .queryParam("query", query)
                .queryParam("display", display)
                .queryParam("start", start)
//                .queryParam("sort", "date")
                .encode(Charset.forName("UTF-8"))
                .build()
                .toUri();

        WebClient webClient = WebClient.builder()
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("X-Naver-Client-Id", CLIENT_ID)
                .defaultHeader("X-Naver-Client-Secret", CLIENT_SECRET)
                .build();

        return webClient.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

    public List<String> getNewsInfo(String path) {
        try {

            List<String> newsInfo = new ArrayList<>();
            String imageUrl = null;
            String category = null;
            Document doc = Jsoup.connect(path).get();
            if (path.contains("https://n.news.naver.com") || path.contains("https://news.naver.com")) {
                Elements newsLists = doc.select("div[id^=img_a1]");
                for (Element news : newsLists) {
                    Element img = news.selectFirst("img");
                    if (img != null) {
                        imageUrl = img.attr("data-src");
                        newsInfo.add(imageUrl);
                        break;
                    }
                }
                Element selectedLink = doc.selectFirst("a.Nitem_link[aria-selected=true]");

                if (selectedLink != null) {
                    Element span = selectedLink.selectFirst("span.Nitem_link_menu");
                    if (span != null) {
                        category = span.text();
                        newsInfo.add(category);
                    }
                }
                if (newsInfo.size() < 2) return null;

                return newsInfo;
            }
            Elements newsLists = doc.select("span[class^=ArticleImage_image_wrap]");
            for (Element news : newsLists) {
                Element img = news.selectFirst("img");
                if (img != null) {
                    imageUrl = img.attr("src");
                    newsInfo.add(imageUrl);
                    break;
                }
            }
            if (path.contains("sports")) newsInfo.add("스포츠");
            newsInfo.add("엔터테인먼트");
            if (newsInfo.size() < 2) return null;

            return newsInfo;
        } catch (Exception e) {
            log.error("[News Service] getThumbnail");
            throw new NotFound("뉴스정보를 가져올 수 없습니다.");
        }
    }

    @Transactional
    public List<String> getUserFavorite(Long userId) {
//        List<Favorite> list = favoriteRepository.findByUserId(userId);
//        if (list.isEmpty()) return null;
//        return list.stream()
//                .map(Favorite::getNewsLink)
//                .collect(Collectors.toList());
        return null;
    }



    public String dateParser(String input) {
        DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH);

        DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("yyyy년 M월 d일 EEEE HH:mm", Locale.KOREAN);

        OffsetDateTime dateTime = OffsetDateTime.parse(input, inputFormatter);
        return dateTime.format(outputFormatter);
    }
}