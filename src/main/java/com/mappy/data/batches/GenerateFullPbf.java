package com.mappy.data.batches;

import com.mappy.data.batches.merge.pbf.OsmMerger;
import com.mappy.data.batches.tomtom.Tomtom2Osm;
import com.mappy.data.batches.tomtom.Tomtom2OsmModule;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.apache.commons.lang3.concurrent.BasicThreadFactory.Builder;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.*;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Splitter.on;
import static com.google.common.base.Throwables.propagate;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.inject.Guice.createInjector;
import static java.lang.Integer.parseInt;

@Slf4j
public class GenerateFullPbf {
    private static final String OSM_PBF_SUFFIX = ".osm.pbf";
    private final OsmMerger osmMerger;
    private final String inputDirectoryPath;
    private final String outputDirectoryPath;
    private final String outputFileName;
    private final ExecutorService executorService;

    public GenerateFullPbf(OsmMerger osmMerger, String inputDirectoryPath, String outputDirectoryPath, String outputFileName, int nbThreads) {
        this.osmMerger = osmMerger;
        this.inputDirectoryPath = inputDirectoryPath;
        this.outputDirectoryPath = outputDirectoryPath;
        this.outputFileName = outputFileName;
        BasicThreadFactory threadFactory = new Builder().namingPattern("mappy-GenerateFullPbf-%d").daemon(false).build();
        executorService = new ThreadPoolExecutor(nbThreads, nbThreads, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(), threadFactory);
    }

    public static void main(String[] args) throws IOException {
        checkArgument(args.length == 5, "Usage: GenerateFullPbf <countryList> <inputDirectoryPath> <outputDirectoryPath> <outputFileName> <threadNumber>");

        String countryList = args[0];
        String inputDirectoryPath = args[1];
        String outputDirectoryPath = args[2];
        String outputFileName = args[3];
        String threadNumber = args[4];

        new GenerateFullPbf( //
                new OsmMerger(), //
                inputDirectoryPath, //
                outputDirectoryPath, //
                outputFileName, //
                parseInt(threadNumber)).run(on(",").trimResults().splitToList(countryList));
    }

    public void run(List<String> countries) throws IOException {
        try {
            List<String> countryPbfFiles = newArrayList();

            for (String country : countries) {
                List<String> zonePbfFiles = newArrayList();
                List<Future<?>> zonesFutures = newArrayList();

                for (String zoneFileName : new File(inputDirectoryPath + "/" + country).list()) {

                    if (zoneFileName.endsWith("_2dbd.shp") || zoneFileName.endsWith("___________nw.shp") || zoneFileName.endsWith("___________fe.shp") || zoneFileName.endsWith("______________a0.shp")) {
                        String zone = zoneFileName.replace("_2dbd.shp", "").replace("___________nw.shp", "").replace("___________fe.shp", "").replace("______________a0.shp", "");

                        String pbfFolderName = outputDirectoryPath + "/" + country + "/pbfFiles";
                        File pbfFolder = new File(pbfFolderName);
                        if (!pbfFolder.exists()) {
                            pbfFolder.mkdirs();
                        }

                        Tomtom2Osm instance = createInjector(new Tomtom2OsmModule(inputDirectoryPath + "/" + country + "/", pbfFolderName, outputDirectoryPath + "/splitter", zone)).getInstance(Tomtom2Osm.class);
                        Future<?> zoneFuture = executorService.submit(() -> {
                            try {
                                zonePbfFiles.add(instance.run());
                            } catch (IOException e) {
                                propagate(e);
                            }
                        });
                        zonesFutures.add(zoneFuture);
                    }
                }
                String countryFile = outputDirectoryPath + "/" + country + "/" + country + OSM_PBF_SUFFIX;
                countryPbfFiles.add(countryFile);
                mergePbfFiles(zonePbfFiles, countryFile, zonesFutures);
            }
            mergePbfFiles(countryPbfFiles, outputDirectoryPath + "/" + outputFileName, newArrayList());

        } finally {
            executorService.shutdown();
        }
    }

    private void mergePbfFiles(List<String> inputPbfFiles, String outputFile, List<Future<?>> tasksToWaitFor) {
        try {
            waitAllDone(tasksToWaitFor);
            osmMerger.merge(inputPbfFiles, outputFile);
        } catch (IOException e) {
            propagate(e);
        }
    }

    private void waitAllDone(List<Future<?>> tasks) {
        tasks.forEach(t -> {
            try {
                t.get();
            } catch (ExecutionException|InterruptedException e) {
                propagate(e);
            }
        });
    }
}