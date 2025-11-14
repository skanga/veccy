package com.veccy.util;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;

import java.io.FileOutputStream;
import java.io.IOException;

public class OfficeTestUtil {

    public static void createDocx(String path, String... text) throws IOException {
        try (XWPFDocument doc = new XWPFDocument()) {
            for (String line : text) {
                XWPFParagraph p = doc.createParagraph();
                XWPFRun run = p.createRun();
                run.setText(line);
            }

            try (FileOutputStream out = new FileOutputStream(path)) {
                doc.write(out);
            }
        }
    }
}
