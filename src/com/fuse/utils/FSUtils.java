package com.fuse.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.spec.KeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.persistence.EntityManager;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.w3c.tidy.Tidy;

import com.fuse.dao.Assessment;
import com.fuse.dao.Category;
import com.fuse.dao.DefaultVulnerability;
import com.fuse.dao.Vulnerability;

public class FSUtils {
	private static String INPUT = "Unvalidated Input";
	private static String SERVER = "Server Misconfiguration";
	private static String CRYPTO = "Weak Cryptography";
	private static String DATAEX = "Data Exposure";
	private static String ACCESS = "Broken Access Control and Session Management";
	private static String PUBLIC = "Publicly Known Vulnerability";
	private static String OUTDATED = "Outdated Libraries and Components";
	private static String UNKNOWN = "Uncategorized";

	public static String jtidy(String html) {
		// figures seems to kill the whole message.
		//html = html.replaceAll("<(/)?figure>", "");

		Tidy tidy = new Tidy();
		InputStream stream = new ByteArrayInputStream(html.getBytes(StandardCharsets.UTF_8));
		// tidy.setXmlOut(true);
		tidy.setQuiet(true);
		tidy.setWord2000(false);
		tidy.setQuoteAmpersand(true);
		tidy.setQuoteMarks(true);
		tidy.setQuoteNbsp(true);
		tidy.setTidyMark(false);
		tidy.setShowErrors(0);
		tidy.setShowWarnings(false);
		tidy.setWraplen(0);
		tidy.setWrapAttVals(false);
		tidy.setPrintBodyOnly(true);
		tidy.setXHTML(true);

		tidy.setOutputEncoding("UTF-8");
		tidy.setInputEncoding("UTF-8");
		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		tidy.parse(stream, baos);
		try {
			String out = new String(baos.toByteArray(), "UTF-8");
			out = out.replaceAll("&nbsp;", " ");
			ArrayList<String> updated = new ArrayList<String>();
			for (String line : out.split("\n")) {
				updated.add(line.replaceAll("^[ ]+", ""));
			}
			out = String.join("", updated);

			return out;
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return new String(baos.toByteArray());
		}
	}

	public static String sanitizeHTML(String html) {
		PolicyFactory policyBuilder = new HtmlPolicyBuilder().allowAttributes("src").onElements("img")
				.allowUrlProtocols("data", "http", "https").allowAttributes("href").onElements("a")
				.allowAttributes("style", "class")
				.onElements("a", "label", "h1", "h2", "h3", "h4", "h5", "h6", "p", "i", "b", "u", "strong", "em",
						"small", "big", "pre", "code", "cite", "samp", "sub", "sup", "strike", "center", "blockquote",
						"hr", "br", "col", "font", "div", "img", "ul", "ol", "li", "dd", "dt", "dl", "tbody", "thead",
						"tfoot", "table", "td", "th", "tr", "colgroup", "fieldset", "legend", "span")
				.allowAttributes("data-changedata", "data-cid", "data-last-change-time", "data-time", "data-userid",
						"data-username", "title")
				.onElements("span").allowAttributes("border", "cellpadding", "cellspacing").onElements("table")
				.allowStandardUrlProtocols()
				.allowElements("a", "label", "h1", "h2", "h3", "h4", "h5", "h6", "p", "i", "b", "u", "strong", "em",
						"small", "big", "pre", "code", "cite", "samp", "sub", "sup", "strike", "center", "blockquote",
						"hr", "br", "col", "font", "div", "img", "ul", "ol", "li", "dd", "dt", "dl", "tbody", "thead",
						"tfoot", "table", "td", "th", "tr", "colgroup", "fieldset", "legend", "del", "ins", "figure",
						"span", "figcaption")
				.toFactory();
		String sanitized = policyBuilder.sanitize(html);

		/// Adding regex for MS Word table copy and paste that mess everything up.
		return sanitized.replaceAll("(style=\".*? )(windowtext)", "$1").replaceAll("style=\"line-height:normal\"", "");

	}

	public static String sanitizeGUID(String guid) {
		String regexGUID = "^[0-9a-zA-Z\\-].*$";
		Matcher guids = Pattern.compile(regexGUID).matcher(guid);
		if (guids.find())
			return guids.group();
		else
			return "";

	}

	public static String sanitizeMongo(String input) {
		return input.replaceAll("[\"'{}:]", "");
	}

	public static boolean checkEmail(String email) {
		String emailRegex = "[A-Z0-9a-z._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}";
		Matcher match = Pattern.compile(emailRegex).matcher(email);
		return match.matches();

	}

	public static List<Vulnerability> sortVulns(List<Vulnerability> vulns) {
		if (vulns == null)
			return new ArrayList<Vulnerability>();

		TreeMap<Long, Vulnerability> sorted = new TreeMap<Long, Vulnerability>();
		for (Vulnerability v : vulns) {
			sorted.put(v.getOverall(), v);
		}
		return new ArrayList<Vulnerability>(sorted.values());

	}

	public static List<Assessment> sortUniqueAssessment(List<Assessment> asmts) {
		if (asmts == null)
			return new ArrayList<Assessment>();
		TreeMap<String, Assessment> sorted = new TreeMap<String, Assessment>();
		for (Assessment a : asmts) {
			sorted.put(a.getAppId(), a);
		}
		return new ArrayList<Assessment>(sorted.values());

	}

	public static void importVulnDB(String proxyurl, int port, String username, String password, EntityManager em)
			throws IOException, ParseException {

		// Create the default Categories
		createCategories(em);
		// Load external resource of vulnerability data.
		String vdbzip = "https://codeload.github.com/factionsecurity/data/zip/master";
		URL obj = new URL(vdbzip);
		HttpURLConnection conn = null;
		if (proxyurl != null) {

			Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyurl, port));
			if (username != null) {
				Authenticator authenticator = new Authenticator() {

					public PasswordAuthentication getPasswordAuthentication() {
						return (new PasswordAuthentication("user", "password".toCharArray()));
					}
				};
				Authenticator.setDefault(authenticator);
			}
			conn = (HttpURLConnection) obj.openConnection(proxy);
		} else {

			conn = (HttpURLConnection) obj.openConnection();
		}

		InputStream is = conn.getInputStream();

		// Unzip the files into memory
		ZipInputStream zis = new ZipInputStream(is);

		ZipEntry entry;
		// Session s = HibHelper.getSessionFactory().openSession();

		while ((entry = zis.getNextEntry()) != null) {
			String name = entry.getName();
			// Only read files in the right path
			if (name.startsWith("data-master/db/") && entry.getSize() != 0l && !name.contains(".gitignore")) {
				byte[] file = new byte[(int) entry.getSize()];
				Scanner sc = new Scanner(zis);
				String jsonStr = "";
				while (sc.hasNextLine()) {
					jsonStr += sc.nextLine();
				}
				// convert new line chars to HTML
				jsonStr = jsonStr.replaceAll("(\\\\n)", "<br>");

				// parse the json files.
				JSONObject json = (JSONObject) new JSONParser().parse(jsonStr);

				List<Category> cats = (List<Category>) em.createQuery("from Category").getResultList();
				HashMap<String, Category> catmap = new HashMap<String, Category>();
				for (Category c : cats) {
					catmap.put(c.getName(), c);
				}
				String title = (String) json.get("title");
				// Input validation findings
				JSONArray tags = (JSONArray) json.get("tags");
				if (tags == null)
					tags = new JSONArray();
				JSONObject fix = (JSONObject) json.get("fix");
				Object ref = json.get("references");

				DefaultVulnerability dv = (DefaultVulnerability) em
						.createQuery("from DefaultVulnerability where name = :name").setParameter("name", title)
						.getResultList().stream().findFirst().orElse(null);
				if (dv == null)
					dv = new DefaultVulnerability();

				dv.setName(title);
				dv.setDescription(
						getDescriptionFromVulnDB((String) (((JSONObject) json.get("description")).get("$ref"))));

				if (ref != null) {
					String addRef = dv.getDescription() + "<br><br><b>References:</b><br>";
					if (ref.getClass().getName().contains("JSONArray")) {
						JSONArray ja = (JSONArray) ref;
						for (int i = 0; i < ja.size(); i++) {
							String url = (String) ((JSONObject) ja.get(i)).get("url");
							String t = (String) ((JSONObject) ja.get(i)).get("title");
							addRef += "<a href='" + url + "'>" + t + "</a><br>";
						}
					} else {
						String url = (String) ((JSONObject) ref).get("url");
						String t = (String) ((JSONObject) ref).get("title");
						addRef += "<a href='" + url + "'>" + t + "</a><br>";
					}
					dv.setDescription(addRef);

				}
				dv.setRecommendation(getFixFromVulnDB((String) (((JSONObject) fix.get("guidance")).get("$ref"))));

				dv.setOverall(setSeverity((String) json.get("severity")));
				dv.setLikelyhood(setSeverity((String) json.get("severity")));
				dv.setImpact(setSeverity((String) json.get("severity")));

				if (tags.contains("session") || tags.contains("authentication"))
					dv.setCategory(catmap.get(ACCESS));
				else if (tags.contains("injection") || tags.contains("xss"))
					dv.setCategory(catmap.get(INPUT));
				else if (tags.contains("csrf"))
					dv.setCategory(catmap.get(ACCESS));
				else if (tags.contains("options"))
					dv.setCategory(catmap.get(SERVER));
				else if (tags.contains("common"))
					dv.setCategory(catmap.get(SERVER));
				else if (title.contains("cookie"))
					dv.setCategory(catmap.get(SERVER));
				else if (title.contains("disclosure") || title.contains("disclosed"))
					dv.setCategory(catmap.get(DATAEX));
				else if (tags.contains("path"))
					dv.setCategory(catmap.get(SERVER));
				else if (tags.contains("upload"))
					dv.setCategory(catmap.get(INPUT));
				else if (title.contains("header"))
					dv.setCategory(catmap.get(SERVER));
				else if (tags.contains("server"))
					dv.setCategory(catmap.get(SERVER));
				else if (title.contains("Insecure client-access policy"))
					dv.setCategory(catmap.get(SERVER));
				else if (title.contains("Allow-Origin header"))
					dv.setCategory(catmap.get(SERVER));
				else if (title.contains("Insecure cross-domain policy"))
					dv.setCategory(catmap.get(SERVER));
				else if (tags.contains("resource"))
					dv.setCategory(catmap.get(SERVER));
				else if (title.contains("auto-complete"))
					dv.setCategory(catmap.get(DATAEX));
				else if (tags.contains("unencrypted") || tags.contains("ssl") || tags.contains("certificate") || tags.contains("cryptography"))
					dv.setCategory(catmap.get(CRYPTO));
				else if (title.contains("XML External Entity"))
					dv.setCategory(catmap.get(INPUT));
				else if (title.contains("configuration"))
					dv.setCategory(catmap.get(SERVER));
				else if (tags.contains("bash"))
					dv.setCategory(catmap.get(SERVER));
				else if (tags.contains("information leak"))
					dv.setCategory(catmap.get(DATAEX));
				else if (tags.contains("credentials"))
					dv.setCategory(catmap.get(ACCESS));
				else if (tags.contains("known cve"))
					dv.setCategory(catmap.get(PUBLIC));
				else if (tags.contains("outdated components"))
					dv.setCategory(catmap.get(OUTDATED));
				else
					dv.setCategory(catmap.get(UNKNOWN));
				em.persist(dv);
				/*
				 * s.getTransaction().begin(); s.save(dv); s.getTransaction().commit();
				 */

			}

		}

	}

	private static int setSeverity(String sev) {
		if (sev.equals("critical"))
			return 5;
		else if (sev.equals("high"))
			return 4;
		else if (sev.equals("medium"))
			return 3;
		else if (sev.equals("low"))
			return 2;
		else if (sev.equals("informational"))
			return 0;
		else if (sev.equals("recommended"))
			return 1;
		else
			return 0;
	}

	private static String convertVDBText(Object text) {
		if (text.getClass().getName().contains("String"))
			return (String) text;
		else {
			String response = "";
			for (int i = 0; i < ((JSONArray) text).size(); i++) {
				response += (String) ((JSONArray) text).get(i) + " ";

			}
			return response;
		}
	}

	private static void createCategories(EntityManager em) {
		// Session s = HibHelper.getSessionFactory().openSession();

		List<String> defaults = new ArrayList<String>();
		defaults.add(INPUT);
		defaults.add(SERVER);
		defaults.add(CRYPTO);
		defaults.add(DATAEX);
		defaults.add(ACCESS);
		defaults.add(PUBLIC);
		defaults.add(UNKNOWN);
		defaults.add(OUTDATED);
		List<Category> cats = (List<Category>) em.createQuery("from Category").getResultList();

		for (String name : defaults) {
			boolean found = false;
			for (Category c : cats) {
				if (c.getName().equals(name)) {
					found = true;
					break;
				}
			}
			if (!found) {
				Category newCat = new Category();
				newCat.setName(name);
				em.persist(newCat);
				/*
				 * s.getTransaction().begin(); s.save(newCat); s.getTransaction().commit();
				 */

			}

		}

	}

	public static String decryptPassword(String password) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			String secret = System.getenv("FACTION_SECRET_KEY");
			byte[] hash = md.digest(secret.getBytes());
			char[] b64hash = Base64.encodeBase64String(hash).toCharArray();

			SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
			KeySpec spec = new PBEKeySpec(b64hash, "f04ce910-bedb-4d8f-a023-4d2441dc0fba".getBytes(), 65536, 256);
			SecretKey tmp = factory.generateSecret(spec);
			SecretKey SecKey = new SecretKeySpec(tmp.getEncoded(), "AES");

			Cipher AesCipher = Cipher.getInstance("AES");
			AesCipher.init(Cipher.DECRYPT_MODE, SecKey);
			byte[] cypherText = Base64.decodeBase64(password);
			byte[] bytePlainText = AesCipher.doFinal(cypherText);
			return new String(bytePlainText);

		} catch (Exception ex) {
			return "";
		}

	}

	public static String encryptPassword(String password) {
		try {

			MessageDigest md = MessageDigest.getInstance("SHA-256");
			String secret = System.getenv("FACTION_SECRET_KEY");
			byte[] hash = md.digest(secret.getBytes());
			char[] b64hash = Base64.encodeBase64String(hash).toCharArray();

			SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
			KeySpec spec = new PBEKeySpec(b64hash, "f04ce910-bedb-4d8f-a023-4d2441dc0fba".getBytes(), 65536, 256);
			SecretKey tmp = factory.generateSecret(spec);
			SecretKey SecKey = new SecretKeySpec(tmp.getEncoded(), "AES");

			Cipher AesCipher = Cipher.getInstance("AES");

			byte[] byteText = password.getBytes();

			AesCipher.init(Cipher.ENCRYPT_MODE, SecKey);
			byte[] byteCipherText = AesCipher.doFinal(byteText);

			return Base64.encodeBase64String(byteCipherText);

		} catch (Exception Ex) {
			Ex.printStackTrace();
			return null;
		}

	}

	public static String generateICSFile(List<String> sendTo, String sendFrom, String Title, String Body) {
		UUID uid = UUID.randomUUID();
		String ics = "BEGIN:VCALENDAR\r\n";
		ics += "VERSION:2.0\r\n";
		ics += "PRODID:-//FuseSoftLLS/Faction//NONSGML v1.0//EN\r\n";
		ics += "BEGIN:VEVENT\r\n";
		ics += "CLASS:PUBLIC\r\n";
		ics += "UID:" + uid.toString() + "\r\n";
		for (String email : sendTo)
			ics += "ATTENDEE;mailto:" + email + "\r\n";
		ics += "X-ALT-DESC;FMTTYPE=text/html:<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 3.2//EN\">\\n<HTML>\\n"
				+ "<BODY>\\n" + Body.replace("\r", "").replace("\n", "\\\\n") + "</BODY></HTML>\r\n";
		ics += "SUMMARY:" + Title + "\r\n";
		// ics+="DESCRIPTION:" + Body.replace("\r", "").replace("\n", "\\\\n") + "\r\n";
		ics += "BEGIN:VALARM\r\n";
		ics += "TRIGGER:-PT15M\r\n";
		ics += "ACTION:DISPLAY\r\n";
		ics += "DESCRIPTION:Reminder\r\n";
		ics += "END:VALARM\r\n";
		ics += "END:VEVENT\r\n";
		ics += "END:VCALENDAR\r\n";

		return ics;
	}

	public static boolean checkEmailDomain(String email) {
		List<String> emails = new ArrayList<String>();
		emails.add(email.trim());
		return checkEmailDomain(emails);
	}

	public static boolean checkEmailDomain(List<String> emails) {
		Properties prop = new Properties();
		InputStream input = null;

		try {
			String path = "/opt/faction/";
			String os = System.getProperty("os.name");
			if (os.contains("win"))
				path = "C:" + path;

			input = new FileInputStream(path + "db.config");

			// load a properties file
			prop.load(input);

			String dString = prop.getProperty("domains", "");
			input.close();
			if ( dString==null || dString.equals("")) {
				return true;
			} else {
				List<String> domains = Arrays.asList(dString.split(";"));

				boolean test = true;
				for (String email : emails) {
					test = domains.stream().anyMatch(e -> email.trim().endsWith("@" + e.trim()));
					// if false we found an email that did not match any domains
					// we should fail the test then.
					if (!test)
						return false;
				}
				return test;
			}
		} catch (Exception ex) {

			ex.printStackTrace();
		}
		return false;
	}

	private static String getDescriptionFromVulnDB(String reference) {
		try {
			String reference_id = reference.replace("#/files/description/", "");
			URL url = new URL("https://raw.githubusercontent.com/factionsecurity/data/master/db/en/description/"
					+ reference_id + ".md");
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			InputStream responseStream = connection.getInputStream();
			String contents = IOUtils.toString(responseStream, StandardCharsets.UTF_8);
			// TODO: Might need to delete this comment. Testing that the new vuln
			// descriptions
			// update correctly
			return contents; // contents.replace("\n\n", "<br/><br/>").replace("\n", " ");
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return "";
		} catch (IOException e) {
			e.printStackTrace();
			return "";
		}
	}

	private static String getFixFromVulnDB(String reference) {
		try {
			String reference_id = reference.replace("#/files/fix/", "");
			URL url;
			url = new URL(
					"https://raw.githubusercontent.com/factionsecurity/data/master/db/en/fix/" + reference_id + ".md");
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			InputStream responseStream = connection.getInputStream();
			String contents = IOUtils.toString(responseStream, StandardCharsets.UTF_8);
			return contents; // contents.replace("\n\n", "<br/><br/>").replace("\n", " ");
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return "";
		} catch (IOException e) {
			e.printStackTrace();
			return "";
		}

	}
	
	public static String getEnv(String ENV_VAR) {
		String var = System.getenv(ENV_VAR);
		return var == null ? "" : var;
	}

}
