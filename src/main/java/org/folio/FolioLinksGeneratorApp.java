package org.folio;

import java.io.File;
import lombok.extern.slf4j.Slf4j;
import org.folio.service.LinksGenerationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Slf4j
@SpringBootApplication
public class FolioLinksGeneratorApp implements CommandLineRunner {
  @Autowired
  private LinksGenerationService service;

  public static void main(String[] args) {
    SpringApplication.run(FolioLinksGeneratorApp.class, args);
  }

  public static void exitWithError(String errorMessage) {
    log.error(errorMessage);
    System.exit(0);
  }

  public static void exitWithMessage(String message) {
    log.info(message);
    System.exit(0);
  }

  @Override
  public void run(String... args) {
    if (args.length != 2) {
      exitWithError("Please specify all parameters: configuration .json file path, authority .mrc file path");
    }

    File configurationFile = new File(args[0]);
    File authorityMrcFile = new File(args[1]);

    service.start(configurationFile, authorityMrcFile);
  }
}