package org.csuc.cli;


import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.io.IoBuilder;
import org.csuc.csv.*;
import org.csuc.marshal.*;
import org.csuc.poi.XLSX2CSV;
import org.csuc.serialize.JaxbMarshal;
import org.csuc.typesafe.semantics.ClassId;
import org.csuc.typesafe.semantics.Semantics;
import org.supercsv.prefs.CsvPreference;
import picocli.CommandLine;
import xmlns.org.eurocris.cerif_1.CfPersType;

import javax.xml.datatype.DatatypeFactory;
import java.io.FileOutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 *
 * @author amartinez
 */
@CommandLine.Command(
        name = "prc-cerif",
        mixinStandardHelpOptions = true,
        version = {"PRC-CSV2XML 0.0.1", "CSUC | (c) 2019"},
        usageHelpAutoWidth = true,
        headerHeading = "Usage:%n%n",
        synopsisHeading = "%n",
        descriptionHeading = "%nDescription:%n%n",
        parameterListHeading = "%nParameters:%n",
        optionListHeading = "%nOptions:%n",
        header = "Excel to CERIF"
)
public class App implements Runnable {

    private Logger logger = LogManager.getLogger();

    @CommandLine.Spec CommandLine.Model.CommandSpec spec;

    @CommandLine.ArgGroup(exclusive = false, multiplicity = "1")
    private Composite composite;

    @CommandLine.Parameters(hidden = true)
    private List<String> allParameters;

    static class Composite {
        @CommandLine.ArgGroup(order = 1, heading = "Input options:%n", multiplicity = "1")
        Input input = new Input();

        @CommandLine.ArgGroup(order = 2, heading = "CSV options:%n")
        CSV csv = new CSV();

        @CommandLine.ArgGroup(order = 3, heading = "CERIF options:%n", multiplicity = "1")
        CERIF cerif = new CERIF();
    }

    static class Input {
        @CommandLine.Option(names = {"-i", "--input"}, required = true, description = "input file", paramLabel = "<PATH>")
        private Path input;

        @CommandLine.Option(
                names = {"-c", "--charset"},
                description = "charset file. Option with optional parameter. (Default: ${DEFAULT-VALUE})%n" +
                        "Candidates: ${COMPLETION-CANDIDATES}",
                paramLabel = "<CHARSET>",
                defaultValue = "UTF-8",
                completionCandidates = StandardCharsetsCandidates.class)
        private String charset = StandardCharsets.UTF_8.name();
    }

    static class CSV {
        @CommandLine.Option(
                names = {"-d", "--delimiter"},
                description = "delimiter",
                paramLabel = "<CHAR>",
                defaultValue = ";")
        private char delimiter = ';';

        @CommandLine.Option(
                names = {"-e", "--endOfLine"},
                description = "endOfLineSymbols",
                paramLabel = "<String>",
                defaultValue = "\n")
        private String endOfLineSymbols = "\n";

        @CommandLine.Option(
                names = {"--deleteOnExit"},
                description = "deleteOnExit",
                paramLabel = "<BOOLEAN>",
                defaultValue = "false")
        private Boolean deleteOnExit = false;

        private Path researcher;
        private Path department;
        private Path relationDepartment;
        private Path researcherGroup;
        private Path relationResearcherGroup;
        private Path project;
        private Path relationProject;
        private Path publication;
        private Path relationPublication;
    }

    static class CERIF {
        @CommandLine.Option(
                names = {"-o", "--out"},
                description = "out",
                paramLabel = "<PATH>",
                defaultValue = "/tmp/example.xml")
        private Path out = Paths.get("/tmp/example.xml");

        @CommandLine.Option(
                names = {"-f", "--formatted"},
                description = "formatted",
                paramLabel = "<BOOLEAN>",
                defaultValue = "false")
        private Boolean formatted = false;

        @CommandLine.Option(
                names = {"-r", "--ruct"},
                description = "ruct (https://www.educacion.gob.es/ruct/home)",
                paramLabel = "<String>",
                required = true)
        private String ruct;
    }


    public void run() {
        XLSX2CSV xlsx2CSV = null;
        try {
            xlsx2CSV = new XLSX2CSV(composite.input.input.toFile(), composite.csv.delimiter, composite.csv.endOfLineSymbols);
            xlsx2CSV.execute();

            xlsx2CSV.getFiles().forEach((key, value) -> {
                switch (key) {
                    case researchers:
                        composite.csv.researcher = value.toPath();
                        break;
                    case departments:
                        composite.csv.department = value.toPath();
                        break;
                    case departments_relations:
                        composite.csv.relationDepartment = value.toPath();
                        break;
                    case research_groups:
                        composite.csv.researcherGroup = value.toPath();
                        break;
                    case research_groups_relations:
                        composite.csv.relationResearcherGroup = value.toPath();
                        break;
                    case projects:
                        composite.csv.project = value.toPath();
                        break;
                    case projects_relations:
                        composite.csv.relationProject = value.toPath();
                        break;
                    case publications:
                        composite.csv.publication = value.toPath();
                        break;
                    case publication_relations:
                        composite.csv.relationPublication = value.toPath();
                        break;
                }
            });

            xmlns.org.eurocris.cerif_1.CERIF cerif = new xmlns.org.eurocris.cerif_1.CERIF();

            GregorianCalendar gregory = new GregorianCalendar();
            gregory.setTime(new Date());
            cerif.setDate(DatatypeFactory.newInstance().newXMLGregorianCalendar(gregory));
            cerif.setSourceDatabase(composite.cerif.ruct);

            List<CfPersType> cfPersTypeList = new ArrayList<>();

            //Researchers
            logger.info("{}", composite.csv.researcher);
            CSVResearcher csvResearcher = new CSVResearcher(composite.csv.researcher.toString(),
                    (new CsvPreference.Builder('"', composite.csv.delimiter, composite.csv.endOfLineSymbols)).build());
            if(Objects.isNull(csvResearcher.readCSV()))   throw new Exception("Researchers not content!");

            csvResearcher.readCSV().forEach(researcher -> {
                MarshalReseracher marshalReseracher =
                        new MarshalReseracher(null, (String) researcher.get(0),
                                null, (String) researcher.get(1),
                                (String) researcher.get(2), null,
                                (String) researcher.get(3), Semantics.getClassId(ClassId.CHECKED));
                cfPersTypeList.add(marshalReseracher);
            });

            //OrgUnits (Department)
            logger.info("{} - {}", composite.csv.department.toString(), composite.csv.relationDepartment.toString());
            CSVDepartment csvDepartment = new CSVDepartment(composite.csv.department.toString(),
                    composite.csv.relationDepartment.toString(),
                    (new CsvPreference.Builder('"', composite.csv.delimiter, composite.csv.endOfLineSymbols)).build());

            Optional.ofNullable(csvDepartment.readCSV()).ifPresent(present-> present.forEach(department -> {
                MarshalDepartment marshalDepartment =
                        new MarshalDepartment(
                                new NameOrTitle((String) department.get(0), null, null),
                                (String) department.get(1),
                                (String) department.get(2),
                                (String) department.get(3),
                                (String) department.get(4),
                                (String) department.get(5),
                                (String) department.get(6),
                                csvDepartment.readCSVRelation(),
                                cfPersTypeList
                        );
                cerif.getCfClassOrCfClassSchemeOrCfClassSchemeDescr().add(marshalDepartment);
            }));

            //OrgUnits (Research Group)
            logger.info("{} - {}", composite.csv.researcherGroup.toString(), composite.csv.relationResearcherGroup.toString());
            CSVResearchGroup csvResearchGroup = new CSVResearchGroup(composite.csv.researcherGroup.toString(),
                    composite.csv.relationResearcherGroup.toString(),
                    (new CsvPreference.Builder('"', composite.csv.delimiter, composite.csv.endOfLineSymbols)).build());

            Optional.ofNullable(csvResearchGroup.readCSV()).ifPresent(present-> present.forEach(group -> {
                MarshalResearchGroup marshalResearchGroup = new MarshalResearchGroup(
                        new NameOrTitle((String) group.get(0), null, null),
                        (String) group.get(1),
                        (String) group.get(2),
                        (String) group.get(3),
                        (String) group.get(4),
                        (String) group.get(5),
                        (String) group.get(6),
                        csvResearchGroup.readCSVRelation(),
                        cfPersTypeList
                );
                cerif.getCfClassOrCfClassSchemeOrCfClassSchemeDescr().add(marshalResearchGroup);
                cfPersTypeList.addAll(marshalResearchGroup.getNewCfPersType());
            }));


            //Projects
            logger.info("{} - {}", composite.csv.project.toString(), composite.csv.relationProject.toString());
            CSVProject csvProject = new CSVProject(composite.csv.project.toString(), composite.csv.relationProject.toString(),
                    (new CsvPreference.Builder('"', composite.csv.delimiter, composite.csv.endOfLineSymbols)).build());
            Optional.ofNullable(csvProject.readCSV()).ifPresent(present-> present.forEach(project -> {
                MarshalProject marshalProject = new MarshalProject(
                        new NameOrTitle((String) project.get(0), null, null),
                        (String) project.get(1),
                        (String) project.get(2),
                        (String) project.get(3),
                        (String) project.get(4),
                        (String) project.get(5),
                        (String) project.get(6),
                        csvProject.readCSVRelation(),
                        cfPersTypeList
                );
                cerif.getCfClassOrCfClassSchemeOrCfClassSchemeDescr().add(marshalProject);
                cfPersTypeList.addAll(marshalProject.getNewCfPersType());
            }));

            //Publications
            logger.info("{} - {}", composite.csv.publication.toString(), composite.csv.relationPublication.toString());
            CSVPublication csvPublication = new CSVPublication(composite.csv.publication.toString(), composite.csv.relationPublication.toString(),
                    (new CsvPreference.Builder('"', composite.csv.delimiter, composite.csv.endOfLineSymbols)).build());

            Optional.ofNullable(csvPublication.readCSV()).ifPresent(present-> present.forEach(publication -> {
                MarshalPublication marshalPublication = new MarshalPublication(
                        new NameOrTitle((String) publication.get(0), null, null),
                        (String) publication.get(1),
                        (String) publication.get(2),
                        (String) publication.get(3),
                        (String) publication.get(4),
                        (String) publication.get(5),
                        (String) publication.get(6),
                        (String) publication.get(7),
                        (String) publication.get(8),
                        (String) publication.get(9),
                        (String) publication.get(10),
                        (String) publication.get(11),
                        (String) publication.get(12),
                        (String) publication.get(13),
                        (String) publication.get(14),
                        csvPublication.readCSVRelation(),
                        cfPersTypeList
                );
                cerif.getCfClassOrCfClassSchemeOrCfClassSchemeDescr().add(marshalPublication);
                cfPersTypeList.addAll(marshalPublication.getNewCfPersType());
            }));

            cerif.getCfClassOrCfClassSchemeOrCfClassSchemeDescr().addAll(cfPersTypeList);

            JaxbMarshal jxb = new JaxbMarshal(cerif, xmlns.org.eurocris.cerif_1.CERIF.class);
            if (Objects.nonNull(composite.cerif.out))
                jxb.marshaller(new FileOutputStream(composite.cerif.out.toFile()), Charset.forName(composite.input.charset), composite.cerif.formatted, false);
            else
                jxb.marshaller(IoBuilder.forLogger(App.class).setLevel(Level.INFO).buildOutputStream(), Charset.forName(composite.input.charset), composite.cerif.formatted, false);

            logger.info("Done - {}", composite.cerif.out);
        } catch (Exception e) {
            logger.error(e);
        }finally {
            if(composite.csv.deleteOnExit)   xlsx2CSV.deleteOnExit();
        }
    }

    public static void main(String[] args) {
        CommandLine cmd = new CommandLine(new App());
        if (args.length == 0)   cmd.usage(System.out);
        else cmd.execute(args);
    }

    static class StandardCharsetsCandidates extends ArrayList<String> {
        StandardCharsetsCandidates() {
            super(Arrays.asList(
                    StandardCharsets.ISO_8859_1.name(),
                    StandardCharsets.US_ASCII.name(),
                    StandardCharsets.UTF_8.name(),
                    StandardCharsets.UTF_16.name(),
                    StandardCharsets.UTF_16BE.name(),
                    StandardCharsets.UTF_16LE.name()
            ));
        }
    }
}
