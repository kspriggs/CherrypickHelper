/*
 * copied from https://gist.github.com/slightfoot/5624590
 * and modified per info on wiki https://en.wikipedia.org/wiki/Digest_access_authentication
 * Tested using gerrit.mot.com
 */
package cherrypickhelper;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import javax.net.ssl.HttpsURLConnection;


public class HttpDigestAuth
{
	public HttpsURLConnection tryAuth(HttpsURLConnection connection, String username, String password)
		throws IOException
	{
		int responseCode = connection.getResponseCode();
		if(responseCode == HttpsURLConnection.HTTP_UNAUTHORIZED){
			connection = tryDigestAuthentication(connection, username, password);
			if(connection == null){
				throw new AuthenticationException();
			}
		}
		return connection;
	}
	
        //Read details at https://en.wikipedia.org/wiki/Digest_access_authentication
	public static HttpsURLConnection tryDigestAuthentication(HttpsURLConnection input, String username, String password)
	{
		String auth = input.getHeaderField("WWW-Authenticate");
		if(auth == null || !auth.startsWith("Digest ")){
			return null;
		}
		final HashMap<String, String> authFields = splitAuthFields(auth.substring(7));
		
		MessageDigest md5 = null;
		try{
			md5 = MessageDigest.getInstance("MD5");
		}
		catch(NoSuchAlgorithmException e){
			return null;
		}
		

		
		String HA1 = null;
                String nonceCount = "00000001"; //hard coding
                String cnonce = "0a4f113b"; //totally made up value
		try{
			md5.reset();
			//kspriggs modified
                        //String ha1str = colonJoiner.join(username, 
			//	authFields.get("realm"), password);
                        //HA1 = MD5(username:realm:password)
                        String realm = authFields.get("realm");
                        String ha1str = username+":"+realm+":"+password;
			md5.update(ha1str.getBytes("ISO-8859-1"));
			byte[] ha1bytes = md5.digest();
			HA1 = bytesToHexString(ha1bytes);
		}
		catch(UnsupportedEncodingException e){
			return null;
		}
		
		String HA2 = null;
		try{
			md5.reset();
			//kspriggs modified
                        //String ha2str = colonJoiner.join(input.getRequestMethod(),
			//	input.getURL().getPath());
                        String ha2str = input.getRequestMethod()+":"+input.getURL().getPath();
			md5.update(ha2str.getBytes("ISO-8859-1"));
			HA2 = bytesToHexString(md5.digest());
		}
		catch(UnsupportedEncodingException e){
			return null;
		}
		
		String HA3 = null;
		try{
			md5.reset();
			//kspriggs modified
                        //String ha3str = colonJoiner.join(HA1, authFields.get("nonce"), HA2);
                        String nonce = authFields.get("nonce");
                        String qop = authFields.get("qop");
                        //String ha3str = HA1+":"+nonce+":"+HA2;
                        String ha3str = HA1+":"+nonce+":"+nonceCount+":"+cnonce+":"+qop+":"+HA2;
			md5.update(ha3str.getBytes("ISO-8859-1"));
			HA3 = bytesToHexString(md5.digest());
		}
		catch(UnsupportedEncodingException e){
			return null;
		}
		
		StringBuilder sb = new StringBuilder(128);
		sb.append("Digest ");
		sb.append("username").append("=\"").append(username                ).append("\",");
		sb.append("realm"   ).append("=\"").append(authFields.get("realm") ).append("\",");
		sb.append("nonce"   ).append("=\"").append(authFields.get("nonce") ).append("\",");
		sb.append("uri"     ).append("=\"").append(input.getURL().getPath()).append("\",");
		sb.append("qop"     ).append('='  ).append("auth"                  ).append(",");
                sb.append("nc"     ).append('='   ).append(nonceCount              ).append(",");
                sb.append("cnonce"  ).append("=\"").append(cnonce                  ).append("\",");
		sb.append("response").append("=\"").append(HA3                     ).append("\"");
		
		try{
			final HttpsURLConnection result = (HttpsURLConnection)input.getURL().openConnection();
			result.addRequestProperty("Authorization", sb.toString());
			return result;
		}
		catch(IOException e){
			return null;
		}
	}
	
	private static HashMap<String, String> splitAuthFields(String authString)
	{
		final HashMap<String, String> fields = new HashMap();
		String[] valuePair;

                for(String keyPair : authString.split(",")){
                    valuePair = keyPair.split("=", 2);
                    valuePair[0] = valuePair[0].replaceAll(" ", "");
                    valuePair[1] = valuePair[1].replaceAll("\"", "");
                    fields.put(valuePair[0], valuePair[1]);
		}
		return fields;
	}
	
	private static final String HEX_LOOKUP = "0123456789abcdef";
	private static String bytesToHexString(byte[] bytes)
	{
		StringBuilder sb = new StringBuilder(bytes.length * 2);
		for(int i = 0; i < bytes.length; i++){
			sb.append(HEX_LOOKUP.charAt((bytes[i] & 0xF0) >> 4));
			sb.append(HEX_LOOKUP.charAt((bytes[i] & 0x0F) >> 0));
		}
		return sb.toString();
	}
	
	public static class AuthenticationException extends IOException
	{
		private static final long serialVersionUID = 1L;
		public AuthenticationException()
		{
			super("Problems authenticating");
		}
	}
}