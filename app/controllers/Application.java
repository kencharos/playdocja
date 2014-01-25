package controllers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import models.Download;

import org.apache.commons.lang.StringUtils;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.User;
import org.eclipse.egit.github.core.client.RequestException;
import org.eclipse.egit.github.core.service.CollaboratorService;
import org.eclipse.egit.github.core.service.UserService;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import play.Logger;
import play.Play;
import play.mvc.Controller;

/**
 * Application controller.
 * 
 * @author garbagetown
 * 
 */
public class Application extends Controller {

    static {
        String hostKey = "http.proxyHost";
        String portKey = "http.proxyPort";
        String host = Play.configuration.getProperty(hostKey);
        String port = Play.configuration.getProperty(portKey);
        if (StringUtils.isNotEmpty(host) && StringUtils.isNotEmpty(port)) {
            System.setProperty(hostKey, host);
            System.setProperty(portKey, port);
        }
    }

    /**
     * index action.
     */
    public static void index() {
        render();
    }

    /**
     * documentation action.
     */
    public static void documentation() {
        redirect(String.format("/documentation/%s/",
                Play.configuration.getProperty("version.latest")));
    }

    /**
     * download action.
     */
    public static void download() {

        Download activator = null;
        Download latest = null;
        List<Download> devels = null;
        List<Download> prevs = null;

        try {
            Document doc = Jsoup.connect("http://www.playframework.com/download").get();

            Elements latests = doc.select("div.latest");
            if (latests.size() != 2) {
                throw new IOException("cannot find latest divisions.");
            }
            Elements changeLogs = doc.select("p.changelogLink");
            if (changeLogs.size() == 0) {
                throw new IOException("cannot find changelog links.");
            }

            // the first div must have latest activator info.
            activator = toDownload(latests.get(0));

            // and the second one must have latest zip info.
            latest = toDownload(latests.get(1));

            // the last paragraph must have previous versions.
            prevs = toDownloads(changeLogs.last());

            // and if there are more than one changelogs, the first one must
            // have development version(s).
            if (changeLogs.size() > 1) {
                devels = toDownloads(changeLogs.first());
            }
        } catch (IOException e) {
            Logger.warn(e.getMessage());
        }
        render(activator, latest, devels, prevs);
    }

    private static Download toDownload(Element latest) {
        Element h2 = latest.select("h2").first();
        Element small = h2.select("small").first();
        Elements cols = latest.select("table").select("tr").select("td");
        if (cols.size() < 2) {
            return null;
        }
        Element a = cols.get(0).select("a").first();

        String url = a.attr("href");
        String txt = h2.ownText().trim();
        String date = small.text().trim();
        String size = cols.get(1).text().trim();
        String misc = a.text().trim();
        return new Download(url, txt, date, size, misc);
    }

    private static List<Download> toDownloads(Element changelog) {
        List<Download> downloads = new ArrayList<Download>();
        Elements rows = changelog.nextElementSibling().select("tr");
        for (Element row : rows) {
            List<Element> cols = row.select("td");
            if (cols.size() < 3) {
                continue;
            }
            Element a = cols.get(0).select("a").first();

            String url = a.attr("href");
            String txt = a.text().trim();
            String date = cols.get(1).html().trim();
            String size = cols.get(2).html().trim();
            downloads.add(new Download(url, txt, date, size, StringUtils.EMPTY));
        }
        return downloads;
    }

    /**
     * changelog action.
     */
    public static void changelog() {
        render();
    }

    /**
     * get-involved action.
     */
    public static void getInvolved() {
        render();
    }

    /**
     * security action.
     */
    public static void security() {
        redirect("/security/vulnerability");
    }

    /**
     * code action.
     */
    public static void code() {
        List<String> zenexities = new ArrayList();
        List<String> typesafes = new ArrayList();
        List<String> lunatechLabs = new ArrayList();
        List<String> others = new ArrayList();
        try {
            Document doc = Jsoup.connect("http://www.playframework.com/code").get();
            Elements elements = doc.select("ul.contributors");
            if (elements.size() >= 4) {
                zenexities = toList(elements.get(0));
                typesafes = toList(elements.get(1));
                lunatechLabs = toList(elements.get(2));
                others = toList(elements.get(3));
            }
        } catch (IOException e) {
            Logger.warn(e.getMessage());
        }
        render(zenexities, typesafes, lunatechLabs, others);
    }

    private static List<String> toList(Element element) {
        List<String> list = new ArrayList();
        Elements elements = element.getElementsByTag("li");
        if (elements != null) {
            for (Element e : elements) {
                list.add(e.toString());
            }
        }
        return list;
    }

    /**
     * about action.
     * 
     * @throws IOException
     */
    public static void about() throws IOException {
        CollaboratorService service = new CollaboratorService();
        String owner = Play.configuration.getProperty("github.owner");
        String name = Play.configuration.getProperty("github.name");
        RepositoryId repository = new RepositoryId(owner, name);
        List<User> collaborators = new ArrayList<User>();
        UserService userService = new UserService();
        try {
            for (User user : service.getCollaborators(repository)) {
                collaborators.add(userService.getUser(user.getLogin()));
            }
        } catch (RequestException e) {
            Logger.warn(e.getMessage());
        }
        render(collaborators);
    }
}