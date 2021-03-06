package com.sudhi.samples.freightcontract.blockchain;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.Serializable;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.security.Security;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;

import org.apache.commons.io.IOUtils;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.hyperledger.fabric.sdk.Enrollment;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.hyperledger.fabric_ca.sdk.HFCAClient;
import org.hyperledger.fabric_ca.sdk.RegistrationRequest;
import org.hyperledger.fabric_ca.sdk.exception.InvalidArgumentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InitializeConfig {
	private static String SHIP_CERT_PATH = "contract-blockchain/network/crypto-config/peerOrganizations/"
			+ "shipperorg.saptm.com/peers/peer0.shipperorg.saptm.com/tls/server.crt";
	private static String CARR_CERT_PATH = "contract-blockchain/network/crypto-config/peerOrganizations/"
			+ "carrierorg.saptm.com/peers/peer0.carrierorg.saptm.com/tls/server.crt";
	private static String LSP_CERT_PATH = "contract-blockchain/network/crypto-config/peerOrganizations/"
			+ "lsporg.saptm.com/peers/peer0.lsporg.saptm.com/tls/server.crt";
	private static String ORD_CERT_PATH = "contract-blockchain/network/crypto-config/ordererOrganizations/"
			+ "saptm.com/orderers/orderer.saptm.com/tls/server.crt";
	private static String SHIP_ADM_CERT_PATH = "contract-blockchain/network/crypto-config/peerOrganizations/"
			+ "shipperorg.saptm.com/users/Admin@shipperorg.saptm.com/msp/signcerts/Admin@shipperorg.saptm.com-cert.pem";
	private static String SHIP_ADM_KEY_PATH = "contract-blockchain/network/crypto-config/peerOrganizations/"
			+ "shipperorg.saptm.com/users/Admin@shipperorg.saptm.com/msp/keystore";
	private static String CARR_ADM_CERT_PATH = "contract-blockchain/network/crypto-config/peerOrganizations/"
			+ "carrierorg.saptm.com/users/Admin@carrierorg.saptm.com/msp/signcerts/Admin@carrierorg.saptm.com-cert.pem";
	private static String CARR_ADM_KEY_PATH = "contract-blockchain/network/crypto-config/peerOrganizations/"
			+ "carrierorg.saptm.com/users/Admin@carrierorg.saptm.com/msp/keystore";
	private static String LSP_ADM_CERT_PATH = "contract-blockchain/network/crypto-config/peerOrganizations/"
			+ "lsporg.saptm.com/users/Admin@lsporg.saptm.com/msp/signcerts/Admin@lsporg.saptm.com-cert.pem";
	private static String LSP_ADM_KEY_PATH = "contract-blockchain/network/crypto-config/peerOrganizations/"
			+ "lsporg.saptm.com/users/Admin@lsporg.saptm.com/msp/keystore";
	private static String SHIP_USR_CERT_PATH = "contract-blockchain/network/crypto-config/peerOrganizations/"
			+ "shipperorg.saptm.com/users/User1@shipperorg.saptm.com/msp/signcerts/User1@shipperorg.saptm.com-cert.pem";
	private static String SHIP_USR_KEY_PATH = "contract-blockchain/network/crypto-config/peerOrganizations/"
			+ "shipperorg.saptm.com/users/User1@shipperorg.saptm.com/msp/keystore";
	private static String CARR_USR_CERT_PATH = "contract-blockchain/network/crypto-config/peerOrganizations/"
			+ "carrierorg.saptm.com/users/User1@carrierorg.saptm.com/msp/signcerts/User1@carrierorg.saptm.com-cert.pem";
	private static String CARR_USR_KEY_PATH = "contract-blockchain/network/crypto-config/peerOrganizations/"
			+ "carrierorg.saptm.com/users/User1@carrierorg.saptm.com/msp/keystore";
	private static String LSP_USR_CERT_PATH = "contract-blockchain/network/crypto-config/peerOrganizations/"
			+ "lsporg.saptm.com/users/User1@lsporg.saptm.com/msp/signcerts/User1@lsporg.saptm.com-cert.pem";
	private static String LSP_USR_KEY_PATH = "contract-blockchain/network/crypto-config/peerOrganizations/"
			+ "lsporg.saptm.com/users/User1@lsporg.saptm.com/msp/keystore";
	private HashMap<String, OrgObject> contractOrgs = new HashMap<>();
	private static InitializeConfig config;
	Logger log = LoggerFactory.getLogger(InitializeConfig.class);
	private OrgObject ordOrg = null;
	
    private static String INVOKEWAITTIME = "100000";
    private static String DEPLOYWAITTIME = "120000";
    private static String PROPOSALWAITTIME = "120000";
	
	private InitializeConfig() {
		// Logging object
		Logger log = LoggerFactory.getLogger(InitializeConfig.class);
		ContractConfig configObject = ContractConfig.getConfig();
		// S3 credentials update
		S3PropertyStore s3Props = new S3PropertyStore();
		// Set the orderer configuration
		ordOrg = new OrgObject();
		String[] ordConfig = configObject.getOrdererConfig();
		ordOrg.setName(ordConfig[1].trim());
		ordOrg.setOrdererLocation(ordConfig[0].trim());
		Properties ordOrgProperties = new Properties();
		File ordCert = Paths.get(ORD_CERT_PATH).toFile();
		ordOrgProperties.put("pemFile", ordCert.getAbsolutePath());
		ordOrgProperties.put("hostnameOverride", "Orderer");
		ordOrgProperties.put("sslProvider", "openSSL");
		ordOrgProperties.put("negotiationType", "TLS");
		ordOrgProperties.put("grpc.NettyChannelBuilderOption.keepAliveTime", new Object[] {5L, TimeUnit.MINUTES});
		ordOrgProperties.put("grpc.NettyChannelBuilderOption.keepAliveTimeout", new Object[] {8L, TimeUnit.SECONDS});
		ordOrgProperties.put("grpc.NettyChannelBuilderOption.keepAliveWithoutCalls", new Object[] {true});
		ordOrg.setOrgProperties(ordOrgProperties);
		
		// Set the Shipper Org configuration
		OrgObject shipperOrg = new OrgObject();
		String[] shipperConfig = configObject.getShipperConfig();
		shipperOrg.setOrdererLocation(ordOrg.getOrdererLocation());
		shipperOrg.setPeerLocation(shipperConfig[0].trim());
		shipperOrg.setName(shipperConfig[1].trim());
		shipperOrg.setMspid(shipperConfig[2].trim());
		shipperOrg.setCaName(shipperConfig[3].trim());
		shipperOrg.setCaLocation(shipperConfig[4].trim());
		shipperOrg.setDomainName(shipperConfig[5].trim());
		shipperOrg.setEventHubLocation(shipperConfig[6].trim());
		Properties shipperOrgProperties = new Properties();
		File shipperCert = Paths.get(SHIP_CERT_PATH).toFile();
		shipperOrgProperties.setProperty("pemFile", shipperCert.getAbsolutePath());
		shipperOrgProperties.setProperty("hostnameOverride", "shipperorg.saptm.com");
		shipperOrgProperties.setProperty("sslProvider", "openSSL");
		shipperOrgProperties.setProperty("negotiationType", "TLS");
		shipperOrgProperties.put("grpc.NettyChannelBuilderOption.maxInboundMessageSize", 9000000);
		shipperOrg.setOrgProperties(shipperOrgProperties);
		try {
			shipperOrg.setCaClient(HFCAClient.createNewInstance(shipperOrg.getCaName(), shipperOrg.getCaLocation(), shipperOrg.getCaProperties()));
		} catch (MalformedURLException e) {
			log.error(e.getMessage());
		} catch (InvalidArgumentException e) {
			log.error(e.getMessage());
		}
		shipperOrg.getCaClient().setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());
		
		//Admin of the Shipper Org
		ContractUser shipperAdmin = new ContractUser(shipperOrg.getName()+"Admin", shipperOrg.getName());
		shipperAdmin.setPeerUser(true);
		shipperAdmin.setEnrollment(getEnrollmentFromFile(getFileFromPath(Paths.get(SHIP_ADM_KEY_PATH).toFile()), Paths.get(SHIP_ADM_CERT_PATH).toFile()));
		shipperAdmin.setMspid(shipperOrg.getMspid());
		shipperOrg.setPeerAdmin(shipperAdmin);
		
		ContractUser shipperUser = new ContractUser(shipperOrg.getName()+"User", shipperOrg.getName());
		shipperUser.setPeerUser(true);
		shipperUser.setEnrollment(getEnrollmentFromFile(getFileFromPath(Paths.get(SHIP_USR_KEY_PATH).toFile()), Paths.get(CARR_USR_CERT_PATH).toFile()));
		shipperUser.setMspid(shipperOrg.getMspid());
		shipperOrg.addUser(shipperUser);
		contractOrgs.put("Shipper", shipperOrg);
		
		// Set the Carrier Org configuration
		OrgObject carrierOrg = new OrgObject();
		String[] carrierConfig = configObject.getCarrierConfig();
		carrierOrg.setOrdererLocation(ordOrg.getOrdererLocation());
		carrierOrg.setPeerLocation(carrierConfig[0].trim());
		carrierOrg.setName(carrierConfig[1].trim());
		carrierOrg.setMspid(carrierConfig[2].trim());
		carrierOrg.setCaName(carrierConfig[3].trim());
		carrierOrg.setCaLocation(carrierConfig[4].trim());
		carrierOrg.setDomainName(carrierConfig[5].trim());
		carrierOrg.setEventHubLocation(carrierConfig[6].trim());
		Properties carrierOrgProperties = new Properties();
		File carrierCert = Paths.get(CARR_CERT_PATH).toFile();
		carrierOrgProperties.setProperty("pemFile", carrierCert.getAbsolutePath());
		carrierOrgProperties.setProperty("hostnameOverride", "carrierorg.saptm.com");
		carrierOrgProperties.setProperty("sslProvider", "openSSL");
		carrierOrgProperties.setProperty("negotiationType", "TLS");
		carrierOrgProperties.put("grpc.NettyChannelBuilderOption.maxInboundMessageSize", 9000000);
		carrierOrg.setOrgProperties(carrierOrgProperties);
		try {
			carrierOrg.setCaClient(HFCAClient.createNewInstance(carrierOrg.getCaName(), carrierOrg.getCaLocation(), carrierOrg.getCaProperties()));
		} catch (MalformedURLException e) {
			log.error(e.getMessage());
		} catch (InvalidArgumentException e) {
			log.error(e.getMessage());
		}
		//Admin of the Carrier Org
		ContractUser carrierAdmin = new ContractUser(carrierOrg.getName()+"Admin", carrierOrg.getName());
		carrierAdmin.setPeerUser(true);
		carrierAdmin.setEnrollment(getEnrollmentFromFile(getFileFromPath(Paths.get(CARR_ADM_KEY_PATH).toFile()), Paths.get(CARR_ADM_CERT_PATH).toFile()));
		carrierAdmin.setMspid(carrierOrg.getMspid());
		carrierOrg.setPeerAdmin(carrierAdmin);
		
		ContractUser carrierUser = new ContractUser(carrierOrg.getName()+"User", carrierOrg.getName());
		carrierUser.setPeerUser(true);
		carrierUser.setEnrollment(getEnrollmentFromFile(getFileFromPath(Paths.get(CARR_USR_KEY_PATH).toFile()), Paths.get(CARR_USR_CERT_PATH).toFile()));
		carrierUser.setMspid(carrierOrg.getMspid());
		carrierOrg.addUser(carrierUser);
		
		contractOrgs.put("Carrier", carrierOrg);
		
		// Set the LSP Org configuration
		OrgObject LSPOrg = new OrgObject();
		String[] LSPConfig = configObject.getLSPConfig();
		LSPOrg.setOrdererLocation(ordOrg.getOrdererLocation());
		LSPOrg.setPeerLocation(LSPConfig[0].trim());
		LSPOrg.setName(LSPConfig[1].trim());
		LSPOrg.setMspid(LSPConfig[2].trim());
		LSPOrg.setCaName(LSPConfig[3].trim());
		LSPOrg.setCaLocation(LSPConfig[4].trim());
		LSPOrg.setDomainName(LSPConfig[5].trim());
		LSPOrg.setEventHubLocation(LSPConfig[6].trim());
		Properties LSPOrgProperties = new Properties();
		File LSPCert = Paths.get(LSP_CERT_PATH).toFile();
		LSPOrgProperties.setProperty("pemFile", LSPCert.getAbsolutePath());
		LSPOrgProperties.setProperty("hostameOverride", "lsporg.saptm.com");
		LSPOrgProperties.setProperty("sslProvider", "openSSL");
		LSPOrgProperties.setProperty("negotiationType", "TLS");
		LSPOrgProperties.put("grpc.NettyChannelBuilderOption.maxInboundMessageSize", 9000000);
		LSPOrg.setOrgProperties(LSPOrgProperties);
		try {
			LSPOrg.setCaClient(HFCAClient.createNewInstance(LSPOrg.getCaName(), LSPOrg.getCaLocation(), LSPOrg.getCaProperties()));
		} catch (MalformedURLException e) {
			log.error(e.getMessage());
		} catch (InvalidArgumentException e) {
			log.error(e.getMessage());
		}
		//Admin of the LSP Org
		ContractUser lspAdmin = new ContractUser(LSPOrg.getName()+"Admin", LSPOrg.getName());
		lspAdmin.setPeerUser(true);
		lspAdmin.setEnrollment(getEnrollmentFromFile(getFileFromPath(Paths.get(LSP_ADM_KEY_PATH).toFile()), Paths.get(LSP_ADM_CERT_PATH).toFile()));
		lspAdmin.setMspid(LSPOrg.getMspid());
		LSPOrg.setPeerAdmin(lspAdmin);
		
		ContractUser LSPUser = new ContractUser(LSPOrg.getName()+"User", LSPOrg.getName());
		LSPUser.setPeerUser(true);
		LSPUser.setEnrollment(getEnrollmentFromFile(getFileFromPath(Paths.get(LSP_USR_KEY_PATH).toFile()), Paths.get(LSP_USR_CERT_PATH).toFile()));
		LSPUser.setMspid(LSPOrg.getMspid());
		LSPOrg.addUser(LSPUser);
		
		contractOrgs.put("LSP", LSPOrg);
	}
	
	public static InitializeConfig getConfig(){
		if(config==null){
			config = new InitializeConfig();
		}
		return config;
	}

	public Collection<OrgObject> getContractOrgs() {
		return Collections.unmodifiableCollection(contractOrgs.values());
	}
	
	public OrgObject getOrderer(){
		return this.ordOrg;
	}
	
	public int getINVOKEWAITTIME() {
		return Integer.parseInt(INVOKEWAITTIME);
	}

	public int getDEPLOYWAITTIME() {
		return Integer.parseInt(DEPLOYWAITTIME);
	}

	public int getPROPOSALWAITTIME() {
		return Integer.parseInt(PROPOSALWAITTIME);
	}
	
	public File getFileFromPath(File directory){
        File[] matches = directory.listFiles((dir, name) -> name.endsWith("_sk"));
        if (null == matches) {
            log.error(format("Matches returned null does %s directory exist?", directory.getAbsoluteFile().getName()));
        }

        if (matches.length != 1) {
        	log.error(format("Expected in %s only 1 sk file but found %d", directory.getAbsoluteFile().getName(), matches.length));
        }
        return matches[0];
	}

	public Enrollment getEnrollmentFromFile(File privateKey, File certificate){
		Reader pemReader;
		final PrivateKeyInfo pemPair;
		Enrollment enroll = null;
		try {
			pemReader = new StringReader(new String(IOUtils.toByteArray(new FileInputStream(privateKey))));
			PEMParser pemParser = new PEMParser(pemReader);
			pemPair = (PrivateKeyInfo) pemParser.readObject();
			pemParser.close();
			PrivateKey pk = new JcaPEMKeyConverter().setProvider(BouncyCastleProvider.PROVIDER_NAME).getPrivateKey(pemPair);
			String cert = new String(IOUtils.toByteArray(new FileInputStream(certificate)), "UTF-8");
			enroll = new UserEnrollment(pk, cert);
		} catch (IOException e) {
			log.error(e.getMessage());
		}
		return enroll;
	}
	
    static {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
    }
    
	public class UserEnrollment implements Enrollment, Serializable{

		private static final long serialVersionUID = 1L;
		private PrivateKey privateKey;
		private String certificate;
		
		public UserEnrollment(PrivateKey pk, String cert){
			this.certificate = cert;
			this.privateKey = pk;
		}
		
		@Override
		public PrivateKey getKey() {
			return this.privateKey;
		}

		@Override
		public String getCert() {
			return this.certificate;
		}
		
	}
}
