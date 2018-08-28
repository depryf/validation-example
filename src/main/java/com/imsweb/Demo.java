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
import com.imsweb.validation.ValidationEngine;
import com.imsweb.validation.ValidatorContextFunctions;
import com.imsweb.validation.XmlValidatorFactory;
import com.imsweb.validation.entities.RuleFailure;
import com.imsweb.validation.entities.SimpleNaaccrLinesValidatable;
import com.imsweb.validation.functions.StagingContextFunctions;

public final class Demo {

    public static void main(String[] args) throws Exception {

        File dataFile = new File("PATH_TO_COMPRESSED_NAACCR_FLAT_FILE");

        // we have to initialize the staging framework since the SEER edits use it...
        Staging csStaging = Staging.getInstance(CsDataProvider.getInstance(CsVersion.LATEST));
        Staging tnmStaging = Staging.getInstance(TnmDataProvider.getInstance(TnmVersion.LATEST));
        Staging eodStaging = Staging.getInstance(EodDataProvider.getInstance(EodVersion.LATEST));
        ValidatorContextFunctions.initialize(new StagingContextFunctions(csStaging, tnmStaging, eodStaging));

        // load the SEER edits and initialize the validation engine
        URL url = Thread.currentThread().getContextClassLoader().getResource("edits/seer-edits.xml");
        if (url == null)
            throw new RuntimeException("Unable to find SEER edits");
        ValidationEngine.initialize(XmlValidatorFactory.loadValidatorFromXml(url));

        // we will use this layout object to read the data file
        NaaccrLayout layout = (NaaccrLayout)LayoutFactory.getLayout(LayoutFactory.LAYOUT_ID_NAACCR_16_INCIDENCE);

        // and finally, run the edits and display some counts
        long start = System.currentTimeMillis();
        System.out.println("Running edits...");
        AtomicInteger recCount = new AtomicInteger(), failuresCount = new AtomicInteger();
        try (LineNumberReader reader = new LineNumberReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(dataFile)), StandardCharsets.UTF_8))) {
            Map<String, String> rec = layout.readNextRecord(reader);
            while (rec != null) {
                recCount.getAndIncrement();
                Collection<RuleFailure> failures = ValidationEngine.validate(new SimpleNaaccrLinesValidatable(Collections.singletonList(rec)));
                failuresCount.addAndGet(failures.size());
                rec = layout.readNextRecord(reader);
            }
        }
        System.out.println("  > done in " + (System.currentTimeMillis() - start) + "ms");
        System.out.println("  > num records: " + recCount.get());
        System.out.println("  > num failures: " + failuresCount.get());
    }

}