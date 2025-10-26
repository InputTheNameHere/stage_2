package org.ulpgc.bd.ingestion;

import io.javalin.Javalin;
import io.javalin.json.JavalinGson;
import org.ulpgc.bd.ingestion.api.IngestionHttpApi;
import org.ulpgc.bd.ingestion.io.HttpDownloader;
import org.ulpgc.bd.ingestion.parser.GutenbergMetaExtractor;
import org.ulpgc.bd.ingestion.parser.GutenbergSplitter;
import org.ulpgc.bd.ingestion.service.IngestionService;

import java.nio.file.Path;
import java.nio.file.Paths;

public class IngestionServiceApp {

    public static void main(String[] args) {
        int port = 7001;
        Path datalake = Paths.get("datalake");
        String parserVersion = "gutenberg-heuristics-8";
        HttpDownloader downloader = new HttpDownloader("IngestionService/1.0 (+mailto:you@example.com)", 6000, 10000);
        GutenbergSplitter splitter = new GutenbergSplitter();
        GutenbergMetaExtractor extractor = new GutenbergMetaExtractor();
        IngestionService service = new IngestionService(datalake, parserVersion, downloader, splitter, extractor);

        Javalin app = Javalin.create(cfg -> cfg.jsonMapper(new JavalinGson())).start(port);
        IngestionHttpApi.register(app, service);
    }
}
