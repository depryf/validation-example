/*
 * Copyright (C) 2014 Information Management Services, Inc.
 */
package com.imsweb;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPInputStream;

import com.imsweb.layout.LayoutFactory;
import com.imsweb.layout.record.fixed.naaccr.NaaccrLayout;
import com.imsweb.staging.Staging;
import com.imsweb.staging.cs.CsDataProvider;
import com.imsweb.staging.cs.CsDataProvider.CsVersion;
import com.imsweb.staging.eod.EodDataProvider;
import com.imsweb.staging.eod.EodDataProvider.EodVersion;
import com.imsweb.staging.tnm.TnmDataProvider;
import com.imsweb.staging.tnm.TnmDataProvider.TnmVersion;
import com.imsweb.validation.ValidationContextFunctions;
import com.imsweb.validation.ValidationEngine;
import com.imsweb.validation.ValidationXmlUtils;
import com.imsweb.validation.edits.seer.SeerRuntimeEdits;
import com.imsweb.validation.entities.RuleFailure;
import com.imsweb.validation.entities.SimpleNaaccrLinesValidatable;
import com.imsweb.validation.functions.StagingContextFunctions;

/**
 * This simple demo shows how to run SEER edits on a given data file.
 * <br/><br/>
 * A production environment should absolutely multi-thread the execution of the edits
 * or the validating a large data file will feel painfully slow.
 * <br/><br/>
 * The engine itself is not multi-threaded internally but is is thread-safe, meaning
 * the calls to the validate() methods can be done in a multi-threaded environment.
 */
public final class DemoSeerEdits {

    public static void main(String[] args) throws Exception {

        File dataFile = new File("PATH_TO_COMPRESSED_NAACCR_FLAT_FILE");

        // we have to initialize the staging framework since the SEER edits use it...
        Staging csStaging = Staging.getInstance(CsDataProvider.getInstance(CsVersion.LATEST));
        Staging tnmStaging = Staging.getInstance(TnmDataProvider.getInstance(TnmVersion.LATEST));
        Staging eodStaging = Staging.getInstance(EodDataProvider.getInstance(EodVersion.LATEST));
        ValidationContextFunctions.initialize(new StagingContextFunctions(csStaging, tnmStaging, eodStaging));

        // load the SEER edits and initialize the validation engine
        ValidationEngine.getInstance().initialize(SeerRuntimeEdits.loadValidator());

        // we will use this layout object to read the data file
        NaaccrLayout layout = (NaaccrLayout)LayoutFactory.getLayout(LayoutFactory.LAYOUT_ID_NAACCR_18_INCIDENCE);

        // and finally, run the edits and display some counts
        long start = System.currentTimeMillis();
        System.out.println("Running edits...");
        AtomicInteger recCount = new AtomicInteger(), failuresCount = new AtomicInteger();
        try (LineNumberReader reader = new LineNumberReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(dataFile)), StandardCharsets.UTF_8))) {
            Map<String, String> rec = layout.readNextRecord(reader);
            while (rec != null) {
                recCount.getAndIncrement();
                Collection<RuleFailure> failures = ValidationEngine.getInstance().validate(new SimpleNaaccrLinesValidatable(Collections.singletonList(rec)));
                failuresCount.addAndGet(failures.size());
                rec = layout.readNextRecord(reader);
            }
        }
        System.out.println("  > done in " + (System.currentTimeMillis() - start) + "ms");
        System.out.println("  > num records: " + recCount.get());
        System.out.println("  > num failures: " + failuresCount.get());
    }

}