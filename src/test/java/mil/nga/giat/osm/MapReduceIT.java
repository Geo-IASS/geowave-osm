package mil.nga.giat.osm;

import mil.nga.giat.geowave.test.GeoWaveDFSTestEnvironment;
import mil.nga.giat.osm.mapreduce.Convert.OSMConversionRunner;
import mil.nga.giat.osm.mapreduce.Ingest.OSMMapperCommandArgs;
import mil.nga.giat.osm.mapreduce.Ingest.OSMRunner;
import mil.nga.giat.osm.osmfeature.types.features.FeatureDefinitionSet;
import mil.nga.giat.osm.parser.OsmPbfParser;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.apache.accumulo.core.client.Connector;
import org.apache.accumulo.core.client.ZooKeeperInstance;
import org.apache.accumulo.core.client.security.tokens.PasswordToken;
import org.apache.accumulo.core.security.Authorizations;
import org.apache.accumulo.server.security.SecurityOperation;
import org.apache.hadoop.fs.ContentSummary;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.util.ToolRunner;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public class MapReduceIT
		extends GeoWaveDFSTestEnvironment
{

	private final static Logger LOGGER = LoggerFactory.getLogger(MapReduceIT.class);


	protected static final String TEST_RESOURCE_DIR = new File("./src/test/data/").getAbsolutePath().toString();
	protected static final String TEST_DATA_ZIP_RESOURCE_PATH = TEST_RESOURCE_DIR + "/" + "andorra-latest.zip";
	protected static final String TEST_DATA_BASE_DIR = new File("./target/data/").getAbsoluteFile().toString();




	@BeforeClass
	public static void setupTestData()
			throws ZipException {
			ZipFile data = new ZipFile(new File(TEST_DATA_ZIP_RESOURCE_PATH));
			data.extractAll(TEST_DATA_BASE_DIR);

	}




	@Test
	public void testIngestOSMPBF()
			throws Exception {
		OSMCommandArgs args = new OSMCommandArgs();
		args.nameNode = NAME_NODE;
		args.ingestDirectory = TEST_DATA_BASE_DIR;
		//OSMPBFStage stager = new OSMPBFStage();
		//stager.StageData(args);
		OsmPbfParser osmPbfParser = new OsmPbfParser();
		osmPbfParser.stageData(args);

		ContentSummary cs = getHDFSFileSummary(args.hdfsBasePath);
		System.out.println("**************************************************");
		System.out.println("Directories: " + cs.getDirectoryCount());
		System.out.println("Files: " + cs.getFileCount());
		System.out.println("Nodes size: " + getHDFSFileSummary(args.getNodesBasePath()).getLength());
		System.out.println("Ways size: " + getHDFSFileSummary(args.getWaysBasePath()).getLength());
		System.out.println("Relations size: " + getHDFSFileSummary(args.getRelationsBasePath()).getLength());
		System.out.println("**************************************************");
		//Assert.assertEquals(cs.getLength(), 204006l);
		System.out.println("finished osmpbf ingest");


		Connector conn = new ZooKeeperInstance(accumuloInstance, zookeeper).getConnector(accumuloUser, new PasswordToken(accumuloPassword));
		Authorizations auth = new Authorizations(new String[] {"public"});
		conn.securityOperations().changeUserAuthorizations(accumuloUser, auth);



		String[] argv = new String[] {"-z", zookeeper, "-i", accumuloInstance, "-au", accumuloUser, "-ap", accumuloPassword, "-n", "osmnamespace", "-v", "public", "-out", args.hdfsBasePath, "-jn", "ConversionTest", "-t", "NODE"};
		ToolRunner.run(CONF, new OSMRunner(), argv);
		System.out.println("finished accumulo ingest Node");

        argv = new String[] {"-z", zookeeper, "-i", accumuloInstance, "-au", accumuloUser, "-ap", accumuloPassword, "-n", "osmnamespace", "-v", "public", "-out", args.hdfsBasePath, "-jn", "ConversionTest", "-t", "WAY"};
        ToolRunner.run(CONF, new OSMRunner(), argv);
        System.out.println("finished accumulo ingest Way");

        argv = new String[] {"-z", zookeeper, "-i", accumuloInstance, "-au", accumuloUser, "-ap", accumuloPassword, "-n", "osmnamespace", "-v", "public", "-out", args.hdfsBasePath, "-jn", "ConversionTest", "-t", "RELATION"};
        ToolRunner.run(CONF, new OSMRunner(), argv);
        System.out.println("finished accumulo ingest Relation");


		FeatureDefinitionSet.initialize(new OSMMapperCommandArgs().getMappingContents());

		//Test conversion
		argv = new String[] {"-z", zookeeper, "-i", accumuloInstance, "-au", accumuloUser, "-ap", accumuloPassword, "-n", "osmnamespace", "-v", "public", "-out", args.hdfsBasePath, "-jn", "ConversionTest", "-t", "RELATION"};
		ToolRunner.run(CONF, new OSMConversionRunner(), argv);
		System.out.println("finished conversion");
		System.out.println("**************************************************");
		System.out.println("**************************************************");
		System.out.println("**************************************************");

	}



	private static ContentSummary getHDFSFileSummary(String filename) throws IOException {
				org.apache.hadoop.fs.Path path = new org.apache.hadoop.fs.Path(filename);
				FileSystem file = path.getFileSystem(CONF);
				ContentSummary cs =  file.getContentSummary(path);
				file.close();
				return cs;
	}


}
