package com.fuse.actions;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import javax.servlet.http.HttpSession;

import org.apache.struts2.ServletActionContext;
import org.apache.struts2.convention.annotation.Action;
import org.apache.struts2.convention.annotation.Namespace;
import org.apache.struts2.convention.annotation.Result;
import org.apache.struts2.convention.annotation.ResultPath;
import org.python.icu.text.SimpleDateFormat;

import com.fuse.dao.AssessmentType;
import com.fuse.dao.AuditLog;
import com.fuse.dao.Campaign;
import com.fuse.dao.Category;
import com.fuse.dao.DefaultVulnerability;
import com.fuse.dao.HibHelper;
import com.fuse.dao.ReportTemplates;
import com.fuse.dao.RiskLevel;
import com.fuse.dao.SystemSettings;
import com.fuse.dao.Teams;
import com.fuse.dao.User;
import com.fuse.tasks.EmailThread;
import com.fuse.tasks.TaskQueueExecutor;
import com.fuse.utils.AccessControl;
import com.fuse.utils.AccessControl.AuthResult;
import com.fuse.utils.FSUtils;
import com.fuse.utils.reporttemplate.ReportTemplate;
import com.fuse.utils.reporttemplate.ReportTemplateFactory;

@Namespace("/")
@ResultPath(value = "/")
@Result(name = "success", location = "index.jsp")
public class Login extends FSActionSupport {

	private String username;
	private String password;
	private String action;
	private String confirm;
	private String first;
	private String last;
	private String email;
	private String adminPassword;
	private String adminUsername;
	private boolean failed = false;
	private String team = "Hacking Team";
	private String message = "";
	private String authorizationUrl;
	private String state;
	private String code;
	private Boolean useSSO;
	private String ssoURL;
	// private static final String PROTECTED_RESOURCE_URL =
	// "https://www.googleapis.com/plus/v1/people/me";

	@Action(value = "index", results = { @Result(name = "createAccount", location = "/WEB-INF/jsp/newInstance.jsp"),
			@Result(name = "failedAuth", location = "/index.jsp"),
			@Result(name = "assessorQueue", type = "redirectAction", location = "portal/Dashboard"),
			@Result(name = "engagement", type = "redirectAction", location = "portal/Engagement"),
			@Result(name = "admin", type = "redirectAction", location = "portal/Users"),
			@Result(name = "calendar", type = "redirectAction", location = "portal/Calendar"),
			@Result(name = "remediation", type = "redirectAction", location = "portal/Remediation") })
	public String execute() {

		if (AccessControl.isNewInstance(em) && action == null) {
			return "createAccount";
		} else if (AccessControl.isAuthenticated(this.JSESSION)) {
			return redirectIt(this.getSessionUser());
		} else if (username != null && password != null && !username.equals("") && !password.equals("")) {
			HibHelper.getInstance().preJoin();
			em.joinTransaction();

			username = username.toLowerCase().trim();
			HttpSession session = ServletActionContext.getRequest().getSession(true);
			AuthResult result = AccessControl.Authenticate(username, password, session, em);
			if (result == AuthResult.SUCCESS) {
				AuditLog.audit(username, this, "Successsfully logged in", AuditLog.Login, false);
				HibHelper.getInstance().commit();
				User user = (User) session.getAttribute("user");
				SystemSettings ss = (SystemSettings) em.createQuery("from SystemSettings").getResultList().stream()
						.findFirst().orElse(null);
				String tier = this.getTier();
				session.setAttribute("tier", tier);
				if (tier == "consultant") {
					session.setAttribute("retestsEnabled", false);
					session.setAttribute("prEnabled", false);
					session.setAttribute("feedEnabled", false);
				} else {
					session.setAttribute("prEnabled", ss.getPeerreview());
					session.setAttribute("feedEnabled", ss.getEnablefeed());
					session.setAttribute("retestsEnabled", true);
				}
				session.setAttribute("title1", ss.getBoldTitle() == null ? "My" : ss.getBoldTitle());
				session.setAttribute("title2", ss.getOtherTitle() == null ? "FACTION" : ss.getOtherTitle());

				return redirectIt(user);
			} else if (result == AuthResult.FAILED_AUTH) {
				AuditLog.notAuthorized(username, this, " Failed Authentication", false);
				HibHelper.getInstance().commit();
				failed = true;
				message = "Username and/or Password was incorrect.";
				return "failedAuth";
			} else if (result == AuthResult.LOCKEDOUT) {
				AuditLog.notAuthorized(username, this, username + " Account Lock Out", false);
				HibHelper.getInstance().commit();

				failed = true;
				message = "Your account has been locked.";
				return "failedAuth";
			} else if (result == AuthResult.INACTIVITY) {
				AuditLog.notAuthorized(username, this, username + " Account was Lock Out due to inactivity", false);
				HibHelper.getInstance().commit();

				failed = true;
				message = "Your account has been locked due to inactivity. Please contact your administrator";
				return "failedAuth";
			} else {
				AuditLog.error(username, this, "Access control result of " + result, AuditLog.Login, false);
				HibHelper.getInstance().commit();

				failed = true;
				message = "Username and/or Password was incorrect.";
				return "failedAuth";
			}

		} else if (action != null && action.equals("create")) {
			if (adminUsername == null || adminUsername.trim().equals("")) {
				this.message = "Username is blank";
				return "createAccount";

			}
			if (adminPassword == null || confirm == null || !adminPassword.equals(confirm)) {
				this.message = "Passwords do not match";
				return "createAccount";
			}
			if (first == null || last == null || first.equals("") || last.equals("")) {
				this.message = "First and/or Last Name are missing";
				return "createAccount";
			}

			if (email == null || email.equals("")) {
				this.message = "Email address is missing";
				return "createAccount";

			}
			
			String errorMessage = AccessControl.checkPassword(adminPassword, confirm);
			if(!errorMessage.equals("")) {
				this.message = errorMessage;
				return "createAccount";
			}

			if (adminPassword.equals(confirm)) {
				if (this.team.equals(""))
					this.team = "Hacking Team";

				User testAdmin = em.createQuery("from User where username = :uname", User.class)
						.setParameter("uname", this.adminUsername).getResultList().stream().findFirst().orElse(null);
				if (testAdmin != null) {
					this.message = "Username already taken";
					return "createAccount";
				}
				HibHelper.getInstance().preJoin();
				em.joinTransaction();
				AccessControl.createAdmin(adminUsername.trim().toLowerCase(), adminPassword, first, last, email, em);

				SystemSettings settings = em.find(SystemSettings.class, 2l);
				Teams theTeam = em.createQuery("from Teams where TeamName = :team", Teams.class)
						.setParameter("team", team).getResultList().stream().findFirst().orElse(null);
				List<AssessmentType> atype = em.createQuery("from AssessmentType").getResultList();
				List<Campaign> campaigns = em.createQuery("from Campaign").getResultList();
				List<RiskLevel> levels = em.createQuery("from RiskLevel").getResultList();
				List<ReportTemplates> reportTemplates = em.createQuery("from ReportTemplates").getResultList();
				Category cat = new Category();
				cat.setName("Uncategorized");
				DefaultVulnerability dv = new DefaultVulnerability();
				dv.setActive(true);
				dv.setCategory(cat);
				dv.setName("Generic Vulnerability");
				dv.setLikelyhood(4);
				dv.setOverall(4);
				dv.setImpact(4);
				dv.setDescription("");
				dv.setRecommendation("");
				em.persist(cat);
				em.persist(dv);
				if (settings == null) {
					settings = new SystemSettings();
					settings.setDefaultStatus("Open");
					List<String> status = new ArrayList<String>();
					status.add("Open");
					status.add("In Progress");
					status.add("On Hold");
					status.add("Closed");
					settings.setStatus(status);
					settings.setDefaultStatus("Open");
				}
				if (theTeam == null)
					theTeam = new Teams();
				if (levels == null || levels.size() == 0) {
					String[] risk = { "Informational", "Recommended", "Low", "Medium", "High", "Critical" };
					for (int i = 0; i < 10; i++) {
						RiskLevel level = new RiskLevel();
						level.setRiskId(i);
						if (i < risk.length)
							level.setRisk(risk[i]);
						em.persist(level);

					}
				}

				if (campaigns == null || campaigns.size() == 0) {
					Campaign camp = new Campaign();
					Date now = new Date();
					SimpleDateFormat sdf = new SimpleDateFormat("yyyy");
					camp.setName("" + sdf.format(now) + " Assessments");
					em.persist(camp);

				}

				theTeam.setTeamName(this.team);
				em.persist(settings);
				em.persist(theTeam);
				User admin = em.createQuery("from User where username = :uname", User.class)
						.setParameter("uname", this.adminUsername.trim()).getSingleResult();
				admin.getPermissions().setAssessor(true);
				admin.getPermissions().setEngagement(true);
				admin.getPermissions().setManager(true);
				admin.getPermissions().setRemediation(true);

				admin.setTeam(theTeam);
				em.persist(admin);

				if (atype == null || atype.isEmpty()) {

					AssessmentType newType = new AssessmentType();
					newType.setType("Security Assessment");
					em.persist(newType);

					if (reportTemplates == null || reportTemplates.isEmpty()) {
						ReportTemplate report = (new ReportTemplateFactory()).getReportTemplate();
						String defaultFileName = report.setup();
						ReportTemplates reportTemplate = new ReportTemplates();
						reportTemplate.setTeam(theTeam);
						reportTemplate.setFilename(defaultFileName);
						reportTemplate.setRetest(false);
						reportTemplate.setType(newType);
						reportTemplate.setName("Sample Template");
						em.persist(reportTemplate);

					}
				}
				AuditLog.audit(adminUsername, this, "Admin Account Created", AuditLog.Login, false);
				HibHelper.getInstance().commit();

			}
			return SUCCESS;

		} else {
			SystemSettings ems = (SystemSettings) em.createQuery("From SystemSettings").getResultList().stream()
					.findFirst().orElse(null);
			if (ems != null && ems.getOauthServer() != null && ems.getSsoEnabled()) {
				this.ssoURL = ems.getOauthServer();
				this.useSSO = true;
			}

			return SUCCESS;
		}

	}

	@Action(value = "reset", results = { @Result(name = "reset", location = "/WEB-INF/jsp/register/reset.jsp") })
	public String reset() {
		return "reset";
	}

	@Action(value = "sendReset", results = { @Result(name = "reset", location = "/WEB-INF/jsp/register/reset.jsp"),
			@Result(name = "gotologin", type = "redirectAction", location = "login"),
			@Result(name = "failedAuth", location = "/index.jsp") })
	public String sendReset() {
		User u = (User) em.createQuery("From User where username = :uname").setParameter("uname", this.getUsername())
				.getResultList().stream().findFirst().orElse(null);
		SystemSettings ems = (SystemSettings) em.createQuery("From SystemSettings").getResultList().stream().findFirst()
				.orElse(null);

		if (u != null && u.getAuthMethod().equals("LDAP")) {
			this.failed = true;
			this.message = "Can't Reset an LDAP account. Please contact your administrator.";
			return "reset";
		}

		if (u != null && !u.getPermissions().isSsouser()) {
			// Check if user is inactive due to number of days inactive
			if (ems != null && ems.getInactiveDays() != null && ems.getInactiveDays() > 30) {
				Date ll = u.getLastLogin();
				// first check if the user has ever logged in.
				if (ll != null || ll.getTime() != 0l) {
					java.util.Calendar backdate = java.util.Calendar.getInstance();
					backdate.add(java.util.Calendar.DATE, -ems.getInactiveDays());
					if (ll.getTime() < backdate.getTimeInMillis()) {
						if (!u.isInActive()) {
							u.setInActive(true);
							HibHelper.getInstance().preJoin();
							em.joinTransaction();
							em.persist(u);
							HibHelper.getInstance().commit();
						}
						return "gotologin";
					}
				}
			}

			String pass = UUID.randomUUID().toString();
			u.setPasshash(AccessControl.HashPass("", pass));
			// Setting username to an empty string prevents logins with GUID.
			// You must register first and create a password to login.
			String message = "Hello " + u.getFname() + " " + u.getLname() + "<br><br>";
			message += "Click the link below to reset your password:<br><br>";
			String url = request.getRequestURL().toString();
			url = url.replace(request.getRequestURI(), "");
			url = url + request.getContextPath() + "/portal/Register?uid=" + pass;
			message += "<a href='" + url + "'>Click here to Reset</a><br>";
			HibHelper.getInstance().preJoin();
			em.joinTransaction();
			em.persist(u);
			HibHelper.getInstance().commit();
			EmailThread emailThread = new EmailThread(u.getEmail(), "Password Reset", message);
			TaskQueueExecutor.getInstance().execute(emailThread);

		}
		return "gotologin";
	}

	private String redirectIt(User user) {
		if (user.getPermissions().isAssessor())
			return "assessorQueue";
		else if (user.getPermissions().isEngagement())
			return "engagement";
		else if (user.getPermissions().isRemediation())

			return "remediation";
		else if (user.getPermissions().isAdmin())
			return "admin";
		else if (user.getPermissions().isManager())
			return "calendar";
		return "login";
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public String getConfirm() {
		return confirm;
	}

	public void setConfirm(String confirm) {
		this.confirm = confirm;
	}

	public String getFirst() {
		return first;
	}

	public void setFirst(String first) {
		this.first = first;
	}

	public String getLast() {
		return last;
	}

	public void setLast(String last) {
		this.last = last;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getAdminPassword() {
		return adminPassword;
	}

	public void setAdminPassword(String adminPassword) {
		this.adminPassword = adminPassword;
	}

	public String getAdminUsername() {
		return adminUsername;
	}

	public void setAdminUsername(String adminUsername) {
		this.adminUsername = adminUsername;
	}

	public boolean isFailed() {
		return failed;
	}

	public void setTeam(String team) {
		this.team = team;
	}

	public String getMessage() {
		return message;
	}

	public String getState() {
		return state;
	}

	public String getCode() {
		return code;
	}

	public void setState(String state) {
		this.state = state;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public Boolean getUseSSO() {
		return useSSO;
	}

	public String getSsoURL() {
		return ssoURL;
	}

	public String getTier() {
		return FSUtils.getEnv("FACTION_TIER");
	}

}