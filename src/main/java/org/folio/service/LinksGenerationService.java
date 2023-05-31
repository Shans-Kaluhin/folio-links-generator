package org.folio.service;

import static org.folio.FolioLinksGeneratorApp.exitWithMessage;
import static org.folio.model.RecordType.MARC_AUTHORITY;
import static org.folio.model.RecordType.MARC_BIB;
import static org.folio.util.FileWorker.deleteFile;
import static org.folio.util.FileWorker.getMappedResourceFile;

import java.io.File;
import me.tongfei.progressbar.ProgressBarBuilder;
import me.tongfei.progressbar.ProgressBarStyle;
import org.folio.client.AuthClient;
import org.folio.client.DataImportClient;
import org.folio.client.EntitiesLinksClient;
import org.folio.client.SRSClient;
import org.folio.model.Configuration;
import org.folio.util.HttpWorker;
import org.springframework.stereotype.Service;

@Service
public class LinksGenerationService {
  private static final String STATUS_BAR_PREFIX = "IMPORT-PROGRESS-BAR  INFO --- [main] : ";
  private MarcConverterService marcConverterService;
  private EntitiesLinksService linksService;
  private DataImportService importService;
  private SRSClient srsClient;

  public static ProgressBarBuilder progressBarBuilder(String title) {
    return new ProgressBarBuilder()
      .setTaskName(STATUS_BAR_PREFIX + title)
      .setStyle(ProgressBarStyle.ASCII)
      .setMaxRenderedLength(STATUS_BAR_PREFIX.length() + 70);
  }

  public void start(File configurationFile, File authorityMrcFile) {
    var configuration = getMappedResourceFile(configurationFile, Configuration.class);
    var httpWorker = new HttpWorker(configuration);

    var authClient = new AuthClient(configuration, httpWorker);
    var linksClient = new EntitiesLinksClient(httpWorker);
    var importClient = new DataImportClient(httpWorker);
    var linkingRuleService = new LinkingRuleService(linksClient);

    srsClient = new SRSClient(httpWorker);
    importService = new DataImportService(importClient);
    linksService = new EntitiesLinksService(linksClient, linkingRuleService);
    marcConverterService = new MarcConverterService(configuration, linkingRuleService);

    httpWorker.setOkapiToken(authClient.authorize());

    var resultMessage = link(authorityMrcFile);
    exitWithMessage(resultMessage);
  }

  private String link(File authorityMrcFile) {
    var authorityJobId = importService.importAuthority(authorityMrcFile);
    var authorities = srsClient.retrieveExternalIdsHolders(authorityJobId, MARC_AUTHORITY);

    var generatedBibs = marcConverterService.generateBibs(authorities);

    var bibJobId = importService.importBibs(generatedBibs);
    var instances = srsClient.retrieveExternalIdsHolders(bibJobId, MARC_BIB);

    linksService.linkRecords(instances);
    deleteFile(generatedBibs);

    return "Records was successfully linked";
  }
}
