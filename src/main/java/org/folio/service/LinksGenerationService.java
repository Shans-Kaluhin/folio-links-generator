package org.folio.service;

import org.folio.client.AuthClient;
import org.folio.client.DataImportClient;
import org.folio.client.EntitiesLinksClient;
import org.folio.client.SRSClient;
import org.folio.model.Configuration;
import org.folio.util.HttpWorker;
import org.springframework.stereotype.Service;

import java.io.File;

import static org.folio.FolioLinksGeneratorApp.exitWithMessage;
import static org.folio.model.RecordType.MARC_AUTHORITY;
import static org.folio.model.RecordType.MARC_BIB;
import static org.folio.util.FileWorker.getMappedResourceFile;

@Service
public class LinksGenerationService {
    private SRSClient srsClient;
    private DataImportService importService;
    private EntitiesLinksService linksService;
    private MarcConverterService marcConverterService;

    public void start(File configurationFile, File authorityMrcFile) {
        var configuration = getMappedResourceFile(configurationFile, Configuration.class);
        var httpWorker = new HttpWorker(configuration);

        var authClient = new AuthClient(configuration, httpWorker);
        var linksClient = new EntitiesLinksClient(httpWorker);
        var importClient = new DataImportClient(httpWorker);

        srsClient = new SRSClient(httpWorker);
        importService = new DataImportService(importClient);
        linksService = new EntitiesLinksService(configuration, linksClient);
        marcConverterService = new MarcConverterService(configuration);

        httpWorker.setOkapiToken(authClient.authorize());

        var resultMessage = link(authorityMrcFile);
        exitWithMessage(resultMessage);
    }

    private String link(File authorityMrcFile) {
        var generatedBibs = marcConverterService.generateBibs();

        var bibJobId = importService.importBibs(generatedBibs);
        var authorityJobId = importService.importAuthority(authorityMrcFile);

        var instances = srsClient.retrieveExternalIdsHolders(bibJobId, MARC_BIB);
        var authorities = srsClient.retrieveExternalIdsHolders(authorityJobId, MARC_AUTHORITY);

        linksService.linkRecords(instances, authorities);
        marcConverterService.writeLinkedIds(instances);

        return "Records was successfully linked";
    }
}
