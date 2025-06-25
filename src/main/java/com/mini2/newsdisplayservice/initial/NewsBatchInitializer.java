package com.mini2.newsdisplayservice.initial;

import com.mini2.newsdisplayservice.domain.entity.News;
import com.mini2.newsdisplayservice.domain.repository.NewsRepository;
import com.mini2.newsdisplayservice.domain.service.NewsSection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class NewsBatchInitializer implements ApplicationRunner {
    private final NewsRepository newsRepository;

    private static final List<NewsSection> SECTIONS = List.of(
            new NewsSection("https://news.naver.com/section/100", "정치"),
            new NewsSection("https://news.naver.com/section/101", "경제"),
            new NewsSection("https://news.naver.com/section/102", "사회"),
            new NewsSection("https://news.naver.com/section/103", "생활/문화"),
            new NewsSection("https://news.naver.com/section/104", "세계"),
            new NewsSection("https://news.naver.com/section/105", "IT/과학")
    );

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        if (newsRepository.count() == 0) {
            log.info("[News Initial Load Start] Initializing news data on application startup (DB is empty).");
            performNewsCrawlingAndSave();
            log.info("[News Initial Load End] News data initialized successfully.");
        } else {
            log.info("[News Initial Load Skip] News data already exists in DB. Skipping initial load on startup.");
        }
    }

    private void performNewsCrawlingAndSave() {
        LocalDateTime batchTime = LocalDateTime.now();
        List<News> newNewsList = new ArrayList<>();

        try {
            for (NewsSection section : SECTIONS) {
                Document doc = Jsoup.connect(section.url())
                        .timeout(10000) // 초기 로드는 혹시 모를 네트워크 지연을 위해 타임아웃을 10초로 설정
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
                                .createdTime(batchTime)
                                .build());
                    }
                }
            }

            // 새로 수집된 뉴스 저장
            newsRepository.saveAll(newNewsList);

            log.info("Successfully loaded {} news items during initial startup.", newNewsList.size());

        } catch (Exception e) {
            log.error("[News Initial Load Error] An error occurred during initial news load: {}", e.getMessage(), e);
            throw new RuntimeException("초기 뉴스 데이터를 가져오는 중 오류가 발생했습니다.", e);
        }
    }

}
