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

import com.imsweb.layout.LayoutFactory;
import com.imsweb.layout.record.fixed.naaccr.NaaccrLayout;
import com.imsweb.staging.Staging;
import com.imsweb.staging.cs.CsDataProvider;
import com.imsweb.staging.cs.CsDataProvider.CsVersion;
import com.imsweb.validation.ValidationEngine;
import com.imsweb.validation.ValidatorContextFunctions;
import com.imsweb.validation.XmlValidatorFactory;
import com.imsweb.validation.entities.RuleFailure;
import com.imsweb.validation.entities.SimpleNaaccrLinesValidatable;
import com.imsweb.validation.entities.Validatable;
import com.imsweb.validation.functions.MetafileContextFunctions;

public final class DemoNaaccrTranslatedEdits {

    @SuppressWarnings("ConstantConditions")
    public static void main(String[] args) throws Exception {

        File dataFile = new File("PATH_TO_COMPRESSED_NAACCR_FLAT_FILE");

        // if defined, only this edit will run...
        String editId = null;

        // we have to initialize the staging framework since the SEER edits use it...
        Staging csStaging = Staging.getInstance(CsDataProvider.getInstance(CsVersion.LATEST));
        ValidatorContextFunctions.initialize(new MetafileContextFunctions(csStaging, null, null));

        // load the SEER edits and initialize the validation engine
        URL url = Thread.currentThread().getContextClassLoader().getResource("edits/naaccr-translated-edits.xml");
        if (url == null)
            throw new RuntimeException("Unable to find edits");
        ValidationEngine.initialize(XmlValidatorFactory.loadValidatorFromXml(url));

        // we will use this layout object to read the data file
        NaaccrLayout layout = (NaaccrLayout)LayoutFactory.getLayout(LayoutFactory.LAYOUT_ID_NAACCR_18_ABSTRACT);

        // and finally, run the edits and display some counts
        long start = System.currentTimeMillis();
        System.out.println("Running edits...");
        AtomicInteger recCount = new AtomicInteger(), failuresCount = new AtomicInteger();
        try (LineNumberReader reader = new LineNumberReader(new InputStreamReader(new FileInputStream(dataFile), StandardCharsets.UTF_8))) {
            Map<String, String> rec = layout.readNextRecord(reader);
            while (rec != null) {
                recCount.getAndIncrement();

                Validatable validatable = new SimpleNaaccrLinesValidatable(Collections.singletonList(rec), null, true);

                Collection<RuleFailure> failures = editId != null ? ValidationEngine.validate(validatable, editId) : ValidationEngine.validate(validatable);
                failuresCount.addAndGet(failures.size());
                rec = layout.readNextRecord(reader);
            }
        }
        System.out.println("  > done in " + (System.currentTimeMillis() - start) + "ms");
        System.out.println("  > num records: " + recCount.get());
        System.out.println("  > num failures: " + failuresCount.get());

    }

}