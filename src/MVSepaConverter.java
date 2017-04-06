import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class MVSepaConverter {
    private static final String MUSIKVEREINIGUNG_1851_METTLACH = "MUSIKVEREINIGUNG 1851 METTLACH";
    private static final String MV_IBAN = "DE94593510400000057331";
    private static final String MV_BIC = "MERZDE55XXX";
    private static final String CdtrSchmeId = "DE55ZZZ00001382005";
    private static final String CURRENCY = "EUR";
    private static final String CSV_SEPARATOR = ";";

    // TODO SET TO CORRECT VALUES BEFORE GENERATING
    private static final double CONTROL_SUM = 2440; // for CheckSum
    private static final LocalDate COLLECTION_DATE = LocalDate.of(2017, Month.APRIL, 13);
    private static final String VERW_ZWECK = "Musikvereinigung 1851 Mettlach eV Mitgliedsbeitrag fuer 2017";
    private static final String INPUT_PATH = "C:/temp/Mitgliedbeitrag2017.csv";
    private static final String OUTPUT_PATH = "C:/temp/Mitgliedbeitrag2017.xml";


    public static void main(String[] args) throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(INPUT_PATH));

        Map<String, Integer> columnNameToIndex = getColumnNameToIndexMapping(lines.get(0));
        List<String> dataLines = lines.subList(1, lines.size());

        FileWriter fileWriter = prepareOutputFile();
        writePrefix(dataLines, fileWriter);
        writeTransactions(columnNameToIndex, dataLines, fileWriter);
        fileWriter.append(XML_SUFFIX);

        fileWriter.close();
    }

    private static Map<String, Integer> getColumnNameToIndexMapping(String headerLine) {
        String[] headerColumns = headerLine.split(CSV_SEPARATOR);
        Map<String, Integer> columnNameToIndex = new HashMap<>();
        for (int i = 0; i < headerColumns.length; i++) {
            columnNameToIndex.put(headerColumns[i], i);
        }
        return columnNameToIndex;
    }

    private static FileWriter prepareOutputFile() throws IOException {
        Path outputPath = Paths.get(OUTPUT_PATH);
        Files.deleteIfExists(outputPath);
        Files.createFile(outputPath);
        return new FileWriter(OUTPUT_PATH);
    }

    private static void writePrefix(List<String> dataLines, FileWriter fileWriter) throws IOException {
        int numberOfRows = dataLines.size();
        String msgId = generateMessageId();
        String creDtTm = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
        String ctrlSum = String.format(Locale.ENGLISH, "%.2f", CONTROL_SUM);
        String collectionDate = COLLECTION_DATE.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        fileWriter.append(String.format(XML_PREFIX, msgId, creDtTm, numberOfRows, ctrlSum, numberOfRows, ctrlSum, collectionDate));
    }

    private static void writeTransactions(Map<String, Integer> columnNameToIndex, List<String> dataLines, FileWriter fileWriter) {
        dataLines.stream().map(csvLine -> csvLine.split(CSV_SEPARATOR))
                .map(csvValues -> String.format(XML_ENTITY_TEMPLATE, String.format(Locale.ENGLISH, "%.2f", Double.valueOf(csvValues[columnNameToIndex.get("BETRAG")])),
                        csvValues[columnNameToIndex.get("MANDATS_REF")],
                        LocalDate.parse(csvValues[columnNameToIndex.get("MANDATS_REF_DATUM")], DateTimeFormatter.ofPattern("dd.MM.yy")).format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                        csvValues[columnNameToIndex.get("NAME")],
                        csvValues[columnNameToIndex.get("IBAN")]))
                .forEach(xml -> {
                    try {
                        fileWriter.append(xml).append(System.lineSeparator());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
    }


    private static String generateMessageId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 27);
    }


    private static final String XML_PREFIX = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
            "<Document xmlns=\"urn:iso:std:iso:20022:tech:xsd:pain.008.003.02\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"urn:iso:std:iso:20022:tech:xsd:pain.008.003.02 pain.008.003.02.xsd\">\n" +
            "    <CstmrDrctDbtInitn>\n" +
            "        <GrpHdr>\n" +
            "            <MsgId>%s</MsgId>\n" +
            "            <CreDtTm>%s</CreDtTm>\n" +
            "            <NbOfTxs>%d</NbOfTxs>\n" +
            "            <CtrlSum>%s</CtrlSum>\n" +
            "            <InitgPty>\n" +
            "                <Nm>" + MUSIKVEREINIGUNG_1851_METTLACH + "</Nm>\n" +
            "            </InitgPty>\n" +
            "        </GrpHdr>\n" +
            "        <PmtInf>\n" +
            "            <PmtInfId>PII486f461825ca47deba92790b92d06978</PmtInfId>\n" + // TODO ???
            "            <PmtMtd>DD</PmtMtd>\n" +
            "            <NbOfTxs>%d</NbOfTxs>\n" +
            "            <CtrlSum>%s</CtrlSum>\n" +
            "            <PmtTpInf>\n" +
            "                <SvcLvl>\n" +
            "                    <Cd>SEPA</Cd>\n" +
            "                </SvcLvl>\n" +
            "                <LclInstrm>\n" +
            "                    <Cd>COR1</Cd>\n" +
            "                </LclInstrm>\n" +
            "                <SeqTp>FRST</SeqTp>\n" +
            "            </PmtTpInf>\n" +
            "            <ReqdColltnDt>%s</ReqdColltnDt>\n" +
            "            <Cdtr>\n" +
            "                <Nm>" + MUSIKVEREINIGUNG_1851_METTLACH + "</Nm>\n" +
            "            </Cdtr>\n" +
            "            <CdtrAcct>\n" +
            "                <Id>\n" +
            "                    <IBAN>" + MV_IBAN + "</IBAN>\n" +
            "                </Id>\n" +
            "            </CdtrAcct>\n" +
            "            <CdtrAgt>\n" +
            "                <FinInstnId>\n" +
            "                    <BIC>" + MV_BIC + "</BIC>\n" +
            "                </FinInstnId>\n" +
            "            </CdtrAgt>\n" +
            "            <ChrgBr>SLEV</ChrgBr>\n" +
            "            <CdtrSchmeId>\n" +
            "                <Id>\n" +
            "                    <PrvtId>\n" +
            "                        <Othr>\n" +
            "                            <Id>" + CdtrSchmeId + "</Id>\n" +
            "                            <SchmeNm>\n" +
            "                               <Prtry>SEPA</Prtry>\n" +
            "                            </SchmeNm>\n" +
            "                        </Othr>\n" +
            "                    </PrvtId>\n" +
            "                </Id>\n" +
            "            </CdtrSchmeId>";


    private static final String XML_ENTITY_TEMPLATE = "\t\t\t<DrctDbtTxInf>\n" +
            "\t\t\t\t<PmtId>\n" +
            "\t\t\t\t\t<EndToEndId>NOTPROVIDED</EndToEndId>\n" +
            "\t\t\t\t</PmtId>\n" +
            "\t\t\t\t<InstdAmt Ccy=\"" + CURRENCY + "\">%s</InstdAmt>\n" +
            "\t\t\t\t<DrctDbtTx>\n" +
            "\t\t\t\t\t<MndtRltdInf>\n" +
            "\t\t\t\t\t\t<MndtId>%s</MndtId>\n" +
            "\t\t\t\t\t\t<DtOfSgntr>%s</DtOfSgntr>\n" +
            "\t\t\t\t\t</MndtRltdInf>\n" +
            "\t\t\t\t</DrctDbtTx>\n" +
            "\t\t\t\t<DbtrAgt>\n" +
            "\t\t\t\t\t<FinInstnId>\n" +
            "\t\t\t\t\t\t<Othr>\n" +
            "\t\t\t\t\t\t\t<Id>NOTPROVIDED</Id>\n" +
            "\t\t\t\t\t\t</Othr>\n" +
            "\t\t\t\t\t</FinInstnId>\n" +
            "\t\t\t\t</DbtrAgt>\n" +
            "\t\t\t\t<Dbtr>\n" +
            "\t\t\t\t\t<Nm>%s</Nm>\n" +
            "\t\t\t\t</Dbtr>\n" +
            "\t\t\t\t<DbtrAcct>\n" +
            "\t\t\t\t\t<Id>\n" +
            "\t\t\t\t\t\t<IBAN>%s</IBAN>\n" +
            "\t\t\t\t\t</Id>\n" +
            "\t\t\t\t</DbtrAcct>\n" +
            "\t\t\t\t<RmtInf>\n" +
            "\t\t\t\t\t<Ustrd>" + VERW_ZWECK + "</Ustrd>\n" +
            "\t\t\t\t</RmtInf>\n" +
            "\t\t\t</DrctDbtTxInf>";

    private static final String XML_SUFFIX = "\t\t</PmtInf>\n" +
            "\t</CstmrDrctDbtInitn>\n" +
            "</Document>";
}
