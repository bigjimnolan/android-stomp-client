package com.eov.androidstomp;

import java.net.SocketTimeoutException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.HashMap;
import java.io.*;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import javax.security.auth.login.LoginException;


/**
 * This method is the bare minimum to get a stomp client working over an ssl connection.  Configuration of the queue to use BKS is also
 * required if you need a secure secure connection.
 * 
 * This MUST be complied with an android compatible version of JAVA (1.6) to work correctly.  It took me too long to remember that :)
 */
public class AndroidStomp {
  //private OutputStream _output;
  //private InputStream _input;
  //private BufferedReader readInput;
  //private SSLSocket  _ssl_socket;
  private String CONNECTION_STRING;
  private SSLSocketFactory sockFact;
  
  //These are the stomp messages.  They full definition and list can be found at: http://stomp.github.io/lo
  private String begin = "BEGIN\r\ntransaction:sendtx\r\n\r\n\000\r\n";
  private String end = "COMMIT\r\ntransaction:sendtx\r\n\r\n\000\r\n";
  private String connectString = "CONNECT\r\nlogin:[userName]\r\npasscode:[password]\r\n\r\n\000\r\n";
  private String writeString = "SEND\r\ndestination:[destination]\r\n\r\n[message]\000\r\n";	
  private String unsubscribeString = "UNSUBSCRIBE\r\ndestination:[queue]\r\ntransaction:senttx\r\n\r\n\000\r\n";  
  private String subscribeString = "SUBSCRIBE\r\ndestination:[queue]\r\ntransaction:sendtx";
  
  private int queuePort;
  private String queueUserName;
  private String queuePassword;

  /**
   * Constructor
   * @param inputStream  		The location of the keystore/key to use for ssl, this must be generated outside of this jar				
   * @param passedInServer		The url of the message queue server
   * @param trustStorePassword	The password for the device's truststore containing the key used for this ssl
   * @param port				The server port for the ssl instance
   * @param queueUserName		The userName to log on to the server
   * @param queuePassword		The password to log on to the server
   */
  public AndroidStomp(InputStream inputStream, String passedInServer,String trustStorePassword, int port, String queueUserName, String queuePassword) {
	  	CONNECTION_STRING = passedInServer;
		queuePort = port;
		this.queueUserName=queueUserName;
		this.queuePassword=queuePassword;
		
	  	try {
			KeyStore trustStore = KeyStore.getInstance("BKS");
			TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			  InputStream trustStoreStream = inputStream; 
			  trustStore.load(trustStoreStream, trustStorePassword.toCharArray());
			  trustManagerFactory.init(trustStore);
			  SSLContext greenSSLContext = SSLContext.getInstance("TLS");
			  
			  greenSSLContext.init(null,trustManagerFactory.getTrustManagers(),null);
			  sockFact = greenSSLContext.getSocketFactory();
			  
		} catch (KeyStoreException e) {		
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {		
			e.printStackTrace();			
		} catch (CertificateException e) {		
			e.printStackTrace();
		} catch (IOException e) {	
			e.printStackTrace();
		} catch (KeyManagementException e) {	
			e.printStackTrace();
		}	  
  }
  
  /**
   * Used to connect to the server
   * This loads the inputstream with some content.  It is the first thing that comes out 
   * when getMessage is called
   * 
   * @param server		URL of the server (www.example.com)
   * @param port		The message queue port (61617)
   * @param login		username for queue
   * @param pass		password for queue   
   * @throws IOException
   * @throws LoginException
   */
  public HashMap<String, Object> connect( String server, int port, String login, String pass )
	  throws IOException, LoginException {	   
	  	HashMap <String, Object> returnObject = new HashMap<String, Object>();
	    SSLSocket _ssl_socket = (SSLSocket) sockFact.createSocket(server, port);	  
	    _ssl_socket.setSoTimeout(15000);	    
	    returnObject.put("inStream", _ssl_socket.getInputStream());	    	
	    returnObject.put("outStream", _ssl_socket.getOutputStream());	  
	    returnObject.put("connection", _ssl_socket);
		
		_ssl_socket.getOutputStream().write(connectString.replace("[userName]",queueUserName).replace("[password]", queuePassword).getBytes());	
		int nRead = 0;
		int tRead = 0;
   		try {
   			while ((nRead = _ssl_socket.getInputStream().read()) > 0) {
   				//	Log.d("Reading", "number:"+nRead); 
   				tRead +=nRead;
   			}
   		} catch (SocketTimeoutException e) {   			  			
   			//do nothing, just make sure the socket is still closed properly
   		}
		return returnObject;
	}
	
	/**
	 * This method is used to send text data to a given queue, it creates a new connection, sends it's message and closes said connection
	 * @param destination		queue name prefaced with "/queue/"
	 * @param message			text string to send to the server
	 * @return					true if method completes
	 * @throws IOException
	 * @throws LoginException 
	 */
	public boolean sendMessage(String destination, String message) throws IOException, LoginException {	
		HashMap<String, Object> streamHash = connect(CONNECTION_STRING, queuePort, queueUserName, queuePassword);
		String inHereWriteString = writeString.replace("[destination]", destination).replace("[message]",message);
		((OutputStream)streamHash.get("outStream")).write(begin.getBytes());
		((OutputStream)streamHash.get("outStream")).write(inHereWriteString.getBytes());
		((OutputStream)streamHash.get("outStream")).write(end.getBytes());
		((SSLSocket)streamHash.get("connection")).close();
		return true;
	}
	
	
	/**
	 * This method is used to create a connection, look for active messages, use them and then close the connection.  The intent here is to not 
	 * have this thing open for long, and, thus not subject to most failures.
	 * 
	 * @param queue			the "/queue/..." to read from
	 * @param ack			the ack mode "client" or "auto"
	 * @return				The message body from the waiting message
	 * @throws IOException
	 * @throws LoginException
	 */
	public String getMessage(String queue, String ack) throws IOException, LoginException {
		
		HashMap<String, Object> streamHash = connect(CONNECTION_STRING, queuePort, queueUserName, queuePassword);
		String inHereSubscribeString = subscribeString.replace("[queue]", queue);
		if (ack != null) {
			inHereSubscribeString+="\r\nack:"+ack;
		}
		inHereSubscribeString += "\r\n\r\n\000\r\n";		
		((OutputStream)streamHash.get("outStream")).write(begin.getBytes());
		((OutputStream)streamHash.get("outStream")).write(inHereSubscribeString.getBytes());		
	    
   		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
   		int nRead;   		

   		try {
   			while ((nRead = ((InputStream)streamHash.get("inStream")).read()) > 0) {
   				//	Log.d("Reading", "number:"+nRead); 
   				buffer.write(nRead);
   			}
   		} catch (SocketTimeoutException safeError) {
   			//This is the "okay" error, basically means there are no messages to read
   		}
   		
   		String receipt="";
  		buffer.flush();  			   
	    String[] Reading = buffer.toString().split("\n");
	    for (String lineIn : Reading) {
	    	if (lineIn.contains("message-id:")) {
	    		receipt=lineIn.split(":",2)[1];
	    		String ackMessage = "ACK\r\nmessage-id:"+receipt+"\r\n\r\n\000\r\n";
	    		((OutputStream)streamHash.get("outStream")).write(ackMessage.getBytes());
	    	}
	    }	  
		
		((OutputStream)streamHash.get("outStream")).write(unsubscribeString.replace("[queue]",  queue).getBytes());
		((OutputStream)streamHash.get("outStream")).write(end.getBytes());
	    ((SSLSocket)streamHash.get("connection")).close();	  
	    String returnVal =Reading.length==0?"No Value":Reading[Reading.length-1];	  
	    return returnVal;
    }
}	
