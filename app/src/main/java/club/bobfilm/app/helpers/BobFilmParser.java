package club.bobfilm.app.helpers;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Environment;
import android.support.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.EOFException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HttpsURLConnection;

import club.bobfilm.app.BuildConfig;
import club.bobfilm.app.R;
import club.bobfilm.app.entity.AppUpdate;
import club.bobfilm.app.entity.Comment;
import club.bobfilm.app.entity.Film;
import club.bobfilm.app.entity.FilmDetails;
import club.bobfilm.app.entity.FilmFile;
import club.bobfilm.app.entity.Section;
import club.bobfilm.app.util.Utils;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by Codex on 22.04.2016.
 */
public class BobFilmParser {
    public static final String FILMS_COUNT_PER_PAGE = "24";
    public static final int ACTION_SECTIONS = 10;
    public static final int ACTION_FILMS = 11;
    public static final int ACTION_FILM_DETAILS = 12;
    public static final int ACTION_SUB_CATEGORIES = 13;
    public static final int ACTION_COMMENTS = 14;
    public static final int ACTION_SEARCH = 15;
    public static final int ACTION_SEARCH_HINTS = 16;
    public static final int ACTION_TEST_PARSE = 17;

    //sharing link
    //https://drive.google.com/file/d/0BwLqqTp54Kpwbzk2bFJrQ25rSTA/view?usp=sharing
    //direct link
    //https://drive.google.com/uc?export=download&id=0BwLqqTp54Kpwbzk2bFJrQ25rSTA
    private static String mAppUpdateUrl;
    public static String mSiteLanguage = "ru";
    private static final String LANG_PAR = "ulang";
    private static int mHttpTimeout;
    public static String mSite;
    private static String mUserAgent;
    private static final String ALLOWED_URI_CHARS = "@#&=*+-_.,:!?()/~'%";
    private static ExecutorService mExecService = Executors.newCachedThreadPool();
    @SuppressLint("StaticFieldLeak")
    private static Context mContext;
    private static String strReviewsDefaultValue;
    private static String strReviewsDefault;
    private static Logger log = LoggerFactory.getLogger(BobFilmParser.class);

    public static void setContext(Context context) {
        BobFilmParser.mContext = context;

        //set default params for parser
        BobFilmParser.mSite = context.getString(R.string.site_url);
        BobFilmParser.mHttpTimeout = context.getResources().getInteger(R.integer.jsoup_timeout);
        BobFilmParser.mUserAgent = context.getString(R.string.jsoup_user_agent);
        BobFilmParser.mAppUpdateUrl = context.getString(R.string.app_update_url);

        strReviewsDefaultValue = context.getResources().getString(R.string.no_reviews);
        strReviewsDefault = context.getResources().getString(R.string.parser_films_responds);
    }

    //unused
    public static void isSiteAvailable(final String url, final LoadListener listener) {
        mExecService.submit(
                new Runnable() {
                    Document docForParsing;
                    String checkSiteWord = mContext.getString(R.string.check_site_available);

                    @Override
                    public void run() {
                        try {
                            docForParsing = Jsoup
                                    .connect(url)
                                    .timeout(mHttpTimeout)
                                    .userAgent(mUserAgent)
                                    .get();
                            if (docForParsing.toString().contains(checkSiteWord)) {
                                listener.OnLoadComplete(true);
                            } else {
                                String siteText = isSiteClosed(docForParsing);
                                if (siteText != null) {
                                    listener.OnLoadComplete(siteText);
                                } else {
                                    listener.OnLoadComplete(false);
                                }
                            }
                        } catch (SocketTimeoutException | SocketException
                                | EOFException | UnknownHostException ex) {
                            listener.OnLoadComplete(false);
                        } catch (Exception ex) {
                            listener.OnLoadError(ex);
                        }
                    }
                }
        );
    }

    public static void loadSite(final String url,
                                final int action,
                                @Nullable final Object parent,
                                final LoadListener listener) {
        mExecService.submit(new Runnable() {
            Document docForParsing;

            @Override
            public void run() {
                //noinspection TryWithIdenticalCatches

                try {
                    log.debug("Try open url={}", url);

                    docForParsing = Jsoup
                            .connect(url)
                            .timeout(mHttpTimeout)
                            .userAgent(mUserAgent)
                            .cookie(LANG_PAR, mSiteLanguage)
                            .get();
                    if (isSiteRestricted(docForParsing)) {
//                        if (BuildConfig.DEBUG) {
//                            docForParsing = openHTMLDocument(url);
//                            log.debug("Restricted, get page from assets");
//                        } else {
                        throw new SocketException(mContext
                                .getString(R.string.msg_connection_failed));
//                        }
                    }
                    parseDocument(listener, action, parent, docForParsing);
                } catch (SocketTimeoutException | SocketException
                        | EOFException | UnknownHostException ex) {
//                    if (BuildConfig.DEBUG) {
//                        parseDocument(listener, action, parent, openHTMLDocument(url));
//                        log.debug("SocketEx, get page from assets");
//                    } else {
                    ex.printStackTrace();
                    listener.OnLoadError(new SocketException(mContext
                            .getString(R.string.msg_connection_failed)));
                    //listener.OnConnectionProblem(mContext.getString(R.string.msg_connection_failed));
//                    }
                } catch (NullPointerException ex) {
                    ex.printStackTrace();
                    listener.OnLoadError(ex);
                } catch (Exception ex) {
//                    Utils.errorWorker(error, log, LOG_TAG, mContext);
                    ex.printStackTrace();
                    listener.OnLoadError(ex);
                }
            }
        });
    }

    private static void parseDocument(LoadListener listener, int action,
                                      Object parent, Document docForParsing) {
        switch (action) {
            case ACTION_SECTIONS:
                listener.OnLoadComplete(parseSections(docForParsing));
                break;
            case ACTION_FILMS:
                listener.OnLoadComplete(parseFilmCards(docForParsing, (Section) parent));
                break;
            case ACTION_FILM_DETAILS:
                listener.OnLoadComplete(parseFilmDetails(docForParsing));
                break;
            case ACTION_SUB_CATEGORIES:
                listener.OnLoadComplete(parseFilmCards(docForParsing, (Section) parent));
                break;
            case ACTION_COMMENTS:
                listener.OnLoadComplete(parseCommentsPage(docForParsing));
                break;
            case ACTION_SEARCH:
                listener.OnLoadComplete(parseSearchResult(docForParsing));
                break;
            case ACTION_SEARCH_HINTS:
                listener.OnLoadComplete(parseSearchHints(docForParsing, (String) parent));
                break;
            case ACTION_TEST_PARSE:
                listener.OnLoadComplete(parseTestSite(docForParsing));
                break;
        }
    }

    //todo need fix for new site
    private static String isSiteClosed(Document doc) {
        String pageTitle = doc.title().toLowerCase();
        if (!pageTitle.equals("") && pageTitle.contains("@ EX.UA".toLowerCase())) {
            Elements div = doc.select("div#fox_news > div[style]");
            String siteText = div.html();
            log.info("text= {}", siteText);
            if (siteText != null && !siteText.isEmpty()) {
                return siteText;
            }
        }
        return null;
    }

    private static boolean isSiteRestricted(Document doc) {
        String pageTitle = doc.toString().toLowerCase();
        return !pageTitle.equals("") && (pageTitle.contains("доступ ограничен".toLowerCase())
                || pageTitle.contains("доступ закрыт".toLowerCase()));
    }

    private static String parseTestSite(Document doc) {
        Elements video = doc.select("span.jwmain");
        String result = video.html();
        log.info("parseTestSite: result: {}", result);
        return result;
    }

    private static String[] parseSearchHints(Document doc, String searchMask) {
        String result = doc.select("body").text();
        log.info("parseSearchHints: result: {}, searchMask {}", result, searchMask);
//        if (result.equalsIgnoreCase("")){
////            return new String[]{""};
//        }
        String[] hints = result.split(" " + searchMask, 5);
        hints[hints.length - 1] = "";
        for (int i = 0; i < hints.length; i++) {
            if (i > 0) {
                hints[i] = searchMask + hints[i];
            }
            log.debug("hints count: {}, resultItem{}: {}", hints.length, i, hints[i]);
        }

//        String[] arrResult = result.split("\\r?\\n");
//        return new ArrayList<>(Arrays.asList(arrResult));
        return hints;
    }

    private static ArrayList<Film> parseSearchResult(Document doc) {
        Elements searchResults = doc.select("table.panel").select("tr:not(#ad_block_1)").select("td");
        if (searchResults == null || searchResults.size() == 0) {
            log.info("parseSearchResult: nothing to parse");
            return new ArrayList<>();
        }
        ArrayList<Film> filmsArr = new ArrayList<>();
        for (Element film : searchResults) {
            if (film.attr("style").equalsIgnoreCase("")) {
                //noinspection TryWithIdenticalCatches
                try {
                    String sPosterUrl = film.select("img").attr("src");
                    String sFilmTitle = film.select("img").attr("alt");
                    String sFilmAbout = film.select("p").first().toString();
                    String sFilmUrl = film.select("a").first().attr("href");
                    String sCreateDate = film.select("small").first().childNodes().get(0).toString();
                    String strReviews = film.select("a.info").text();
                    boolean hasArticles = false;
                    String sArticlesMask = mContext.getResources().getString(R.string.parser_films_articles_mask);
                    if (strReviews.toLowerCase().contains(sArticlesMask.toLowerCase())) {
                        hasArticles = true;
                        log.debug("{} sArticlesMask: {}, strReviews: {}, true",
                                sFilmTitle, sArticlesMask, strReviews);
                    }
                    String sReviews = "";
                    String sReviewsCount = "";
                    if (strReviews.contains(":")) {
                        sReviews = strReviews.substring(0, strReviews.indexOf(":") + 2);
                        sReviewsCount = strReviews.substring(strReviews.indexOf(":") + 2);
                    } else {
                        log.warn("string {} not contains \':\'", strReviews);
                    }
                    sReviews = (sReviewsCount.equalsIgnoreCase("")
                            || sReviewsCount.equalsIgnoreCase("0")) ?
                            strReviewsDefault + strReviewsDefaultValue : strReviews;
                    String sReviewsUrl = film.select("a.info").attr("href");

                    final Film filmObj = new Film();

                    filmObj.setPosterUrl(sPosterUrl);
                    filmObj.setFilmTitle(sFilmTitle);
                    filmObj.setFilmAbout(sFilmAbout);
                    filmObj.setFilmUrl(sFilmUrl);
                    filmObj.setCreateDate(sCreateDate);
                    filmObj.setReviews(sReviews);
                    filmObj.setHasArticle(hasArticles);
                    filmObj.setReviewsUrl(sReviewsUrl);

                    filmsArr.add(filmObj);
                } catch (Exception ex) {
                    if (BuildConfig.DEBUG) {
                        ex.printStackTrace();
                    } else {
                        log.error(Utils.getErrorLogHeader() + new Object() {
                        }.getClass().getEnclosingMethod().getName(), ex);
                    }
                }
            }
        }
        return filmsArr;
    }

    private static ArrayList<Comment> parseCommentsPage(Document doc) {
        Elements elComments = doc.select("table.comment").select("td[style^=padding-left]");
        if (elComments == null || elComments.size() == 0) {
            return new ArrayList<>();
        }
        ArrayList<Comment> commentsArr = new ArrayList<>();
        for (Element comment : elComments) {
            //skip item if create date or comment body and comment title is empty
            if (comment.select("small").text().equalsIgnoreCase("")
                    || (comment.select("p").first().text().equalsIgnoreCase("")
                    && comment.select("a[href^=/view_comments/").text().equalsIgnoreCase("")))
                continue;
            //noinspection TryWithIdenticalCatches
            try {
                String sLeftPadding = Utils.extractDigitsFromString(comment.select("td").attr("style"));
                String sAvatarUrl = comment.select("img").attr("src");
                boolean bIsReplica = sAvatarUrl.contains("i/comment.gif");
                String sCommentLink = comment.select("a[href^=/view_comments/").attr("href");
                String sCommentTitle = comment.select("a[href^=/view_comments/").text();
                String sCommentBodyHTML = comment.select("p").first().text();
                String sUser = comment.select("a[href^=/user/").text();
                String sUserProfileLink = comment.select("a[href^=/user/").attr("href");
                String sCreateDate = comment.select("small").text();
                if (sCreateDate.contains(",")) {
                    sCreateDate = sCreateDate.substring(0,
                            sCreateDate.indexOf(",", sCreateDate.indexOf(",") + 1));
                } else {
                    log.warn("Data {} not contains \',\'", sCreateDate);
                }
                final Comment commentsObj = new Comment();
                commentsObj.setReplica(bIsReplica);
                commentsObj.setLeftOffset(sLeftPadding);
                commentsObj.setUserProfileUrl(sUserProfileLink);
                commentsObj.setCommentUrl(sCommentLink);
                commentsObj.setAvatarUrl(sAvatarUrl);
                commentsObj.setCommentTitle(sCommentTitle);
                commentsObj.setCommentBodyHTML(sCommentBodyHTML);
                commentsObj.setUser(sUser);
                commentsObj.setCreateDate(sCreateDate);
                commentsArr.add(commentsObj);
            } catch (Exception ex) {
                if (BuildConfig.DEBUG) {
                    ex.printStackTrace();
                } else {
                    log.error(Utils.getErrorLogHeader() + new Object() {
                    }.getClass().getEnclosingMethod().getName(), ex);
                }
            }
        }
        return commentsArr;
    }

    private static ArrayList<Section> parseSections(Document doc) {
        Elements sections = doc.select("div.bl_nav").select("a:not(a[href^=javascript]");
        ArrayList<Section> sectionsArr = new ArrayList<>();
        //noinspection TryWithIdenticalCatches
        try {
            int position = 0;
            for (Element section : sections) {
                String sTitle = section.select("a").text();
                String sUrl = section.select("a").attr("href");
                Section sectionObj = new Section();
                sectionObj.setmSectionPosition(position);
                sectionObj.setSectionTitle(sTitle);
                sectionObj.setSectionUrl(sUrl);
                sectionsArr.add(sectionObj);
                position++;
            }
            //log.debug("get " + String.valueOf(sectionsArr.size()) + " sections");
        } catch (Exception ex) {
            if (BuildConfig.DEBUG) {
                ex.printStackTrace();
            } else {
                log.error(Utils.getErrorLogHeader() + new Object() {
                }.getClass().getEnclosingMethod().getName(), ex);
            }
        }
        return sectionsArr;
    }

    private static FilmDetails parseFilmDetails(Document doc) {
        FilmDetails filmDetailsObj = new FilmDetails();
        //noinspection TryWithIdenticalCatches
        try {
            Elements filmDetails = doc.select("div#dle-content").select("div.sh0");

            if (filmDetails == null) {
                return parseArtistDetails(doc);
            }
            String sFilmTitle = filmDetails.select(".sh2").select("h1").text();
            String sBigPosterUrl = filmDetails.select("div.posterfull_img").select("img").attr("src");
            sBigPosterUrl = sBigPosterUrl.contains("http") ? sBigPosterUrl : mSite + sBigPosterUrl;
            String sFilmDetails = filmDetails.select("div.fulldisc").select("div[id^=news-id]").html();

            Elements sFilmFullInfo = filmDetails.select("ul.fullinfo>li");

            String sFilmYear = sFilmFullInfo.select("li:has(span:contains(Год выпуска:))").html();
            String sFilmCountry = sFilmFullInfo.select("li:has(span:contains(Страна:))").html();
            String sFilmSlogan = sFilmFullInfo.select("li:has(span:contains(Слоган:))").html();
            String sFilmGenre = sFilmFullInfo.select("li:has(span:contains(Жанр:))").html();
            String sFilmQuality = sFilmFullInfo.select("li:has(span:contains(Качество:))").html();
            String sFilmDuration = sFilmFullInfo.select("li:has(span:contains(Прод-сть:))").html();
            String sFilmTranslation = sFilmFullInfo.select("li:has(span:contains(Перевод:))").html();
            String sFilmPremiere = sFilmFullInfo.select("li:has(span:contains(Премьера:))").html();
            String sFilmProducer = sFilmFullInfo.select("li:has(span:contains(Режиссер:))").html();

            String sFilmActors = filmDetails.select("div.n_actors").html();

            String sFilmCreateDate = filmDetails.select(".sh9").text();
            if (sFilmCreateDate.contains(",")) {
                sFilmCreateDate = sFilmCreateDate.substring(sFilmCreateDate.indexOf(":") + 2);
            } else {
                log.warn("Date {} not contains \',\'", sFilmCreateDate);
            }

            String sFilmReviewsCount = doc.select("div.sh13_full").select(".item").select(".num").text();
            List<Comment> commentsList = new ArrayList<>();
            Elements elsFilmReviews = doc.select("div[class^=comm0]");
            for (Element comment : elsFilmReviews) {
                String userNameDate = comment.select("span.comm5").text();
                String userName = userNameDate.substring(0, userNameDate.indexOf("(") - 1);
                String commentCreateDate = userNameDate.substring(userNameDate.indexOf("(") + 1).replace(")", "");
                String avatarUrl = comment.select("img.comm4").attr("src");
                avatarUrl = avatarUrl.contains("http") ? avatarUrl : mSite + avatarUrl;
                String commentTextHtml = comment.select("div.comm9").html();
                commentsList.add(new Comment(avatarUrl, commentCreateDate, userName, commentTextHtml));
            }

            // need parse and save to comments object
            Elements sFilmReviewsUrl = doc.select("div[class^=comm0]");


            String sPlaylistUrl = doc.select("param[name=flashvars]").attr("value");
            if (sPlaylistUrl.contains("&pl")) {
                sPlaylistUrl = sPlaylistUrl.substring(sPlaylistUrl.indexOf("&pl") + 4);
                sPlaylistUrl = sPlaylistUrl.contains("http") ? sPlaylistUrl : mSite + sPlaylistUrl;

            } else {
                log.warn("Playlist {} not contains \'&\'", sPlaylistUrl);
                sPlaylistUrl = null;
            }
            List<FilmFile> filesList = new ArrayList<>();
            JsonObject jRequest;
            if (sPlaylistUrl != null) {
                jRequest = getDataFromServer(sPlaylistUrl);
                JsonArray playlist = jRequest.getAsJsonArray("playlist");
                filesList = new Gson().fromJson(playlist, new TypeToken<List<FilmFile>>() {
                }.getType());
                log.info("files count {}", filesList.size());
            }

            for (FilmFile file : filesList) {
                String fileUrl = file.getFileUrl();
                String fileName = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
                file.setFileName(fileName);
            }

//            Elements sFiles = doc.select("param[name=flashvars]").attr("value");
//            sFiles = sFiles.select(":not(a[title~=(?i)\\.(png|jpe?g|bmp|gif)])");
//            HashMap<String, String> hLightFiles = getLightFiles(doc);
//            String sLightFileUrl = null;
//            String sLightFileName = null;
//            //noinspection MismatchedQueryAndUpdateOfCollection
//            List<FilmFile> filesList = new ArrayList<>();
//            for (int i = 0; i < sFiles.size(); i++) {
//                String sFileUrl = sFiles.get(i).select("a").attr("href");
//                String sFileName = sFiles.get(i).select("a").text();
//                if (hLightFiles != null) {
//                    String htmlString = TextUtils.htmlEncode(sFileName);
//                    sLightFileUrl = hLightFiles.get(htmlString);
//                    if (sLightFileUrl != null && sLightFileUrl.contains("/")) {
//                        sLightFileName = sLightFileUrl.substring(sLightFileUrl.lastIndexOf("/") + 1);
//                        if (sLightFileName.contains(".")) {
//                            sLightFileName = sFileName.substring(0, sFileName.lastIndexOf("."))
//                                    + "_light" +
//                                    sLightFileName.substring(sLightFileName.lastIndexOf("."));
//                            filesList.add(new FilmFile(sFileName, sFileUrl,
//                                    sBigPosterUrl, sLightFileUrl, sLightFileName));
//                        } else {
//                            log.warn("error rename file to light version from {}", sFileName);
//                        }
//                    } else {
//                        log.warn("string \'{}\' not contains \'/\'", sLightFileUrl);
//                    }
//                }
//                if (sLightFileName == null) {
//                    filesList.add(new FilmFile(sFileName, sFileUrl, sBigPosterUrl));
//                }
//            }
            filmDetailsObj.setBigPosterUrl(sBigPosterUrl);
            filmDetailsObj.setFilmTitle(sFilmTitle);
            filmDetailsObj.setFilmYear(sFilmYear);
            filmDetailsObj.setFilmCreateDate(sFilmCreateDate);
            filmDetailsObj.setFilmReviews(sFilmReviewsCount);
            filmDetailsObj.setFilmComments(commentsList);
            //filmDetailsObj.setFilmReviewsUrl(sFilmReviewsUrl);
            filmDetailsObj.setFilmDetailsHTML(sFilmDetails);
            filmDetailsObj.setFilmFiles(filesList);
        } catch (Exception ex) {
            if (BuildConfig.DEBUG) {
                ex.printStackTrace();
            } else {
                log.error(Utils.getErrorLogHeader() + new Object() {
                }.getClass().getEnclosingMethod().getName(), ex);
            }
        }
        return filmDetailsObj;
    }

    private static List<Film> parseFilmCards(Document doc, Section category) {
        if (category == null) {
            log.error("parseFilmCards: category is null");
            return new ArrayList<>();
        }
        Elements pages = doc.select("div.pagination");
        Elements nextPage = pages.select(".page_btn.next_page");
        String nextPageUrl = nextPage.attr("href");
        if (nextPageUrl != null) {
            log.info("Set nextPageUrl: {}", nextPageUrl);
            category.setNextPageUrl(nextPageUrl);
        } else {
            category.setNextPageUrl("");
        }


        String searchCategoryId = doc.select("input[name=original_id]").attr("value");
        if (searchCategoryId != null) {
            category.setSearchId(searchCategoryId);
        }


        Elements tableInclude = doc.select("div#dle-content");
        Elements films = tableInclude.select("div.sh15");
        if (films == null || films.size() == 0) {
            return new ArrayList<>();
        }
        ArrayList<Film> filmsArr = new ArrayList<>();
        //noinspection TryWithIdenticalCatches
        try {
            for (Element film : films) {
                String sPosterUrl = film.select(".postershort").select("img").attr("src");
                sPosterUrl = sPosterUrl.contains("http") ? sPosterUrl : mSite + sPosterUrl;
                String sFilmTitle = film.select(".postershort").select("img").attr("title");
                String sFilmUrl = film.select(".postershort").select("a").attr("href");
                String sFilmShortDescription = film.select("div.shortdisc").text();

                String sCreateDate = film.select("span.sh9").text();
//                if (sCreateDate.contains(",")) {
//                    sCreateDate = sCreateDate.substring(sCreateDate.indexOf(":") + 2);
//                } else {
//                    log.debug("Date {} not contains \',\'", sCreateDate);
//                }

                String sNextPageUrl = "";
                final Film filmObj = new Film();
                filmObj.setPosterUrl(sPosterUrl);
                filmObj.setFilmTitle(sFilmTitle);
                filmObj.setFilmUrl(sFilmUrl);
                filmObj.setCreateDate(sCreateDate);
                filmObj.setFilmAbout(sFilmShortDescription);
                filmsArr.add(filmObj);
            }
        } catch (Exception ex) {
            if (BuildConfig.DEBUG) {
                ex.printStackTrace();
            } else {
                log.error(Utils.getErrorLogHeader() + new Object() {
                }.getClass().getEnclosingMethod().getName(), ex);
            }
        }
        return filmsArr;
    }

    private static HashMap<String, String> getLightFiles(Document doc) {
        Elements aLightVer = doc.select("script");
        Element eLightVer = null;
        if (!aLightVer.toString().toLowerCase().contains("newPlaylist".toLowerCase())) {
            //eLightVer = openHTMLDocument("files_light").select("script").first();
            log.info("no light version of files");
        } else {
            for (Element script : aLightVer) {
                if (script.toString().toLowerCase().contains("newPlaylist".toLowerCase())) {
                    eLightVer = script;
                    break;
                }
            }
        }
        if (eLightVer == null) {
            log.debug("eLightVer = null");
            return null;
        }
        String baseString = eLightVer.toString();
        if (!baseString.contains("$.each")) {
            log.warn("\'$.each\' not found in string \'{}\'", baseString);
            return null;
        }
        String baseString1 = baseString.substring(0, baseString.indexOf("$.each"));
        String[] resultString = baseString1.split("\\[\\{");

        List<String> strings = new ArrayList<>();
        for (String elem : resultString) {
            if (elem.contains("}]")) {
                strings.add(elem.substring(0, elem.indexOf("}]")).replace("{", ""));
            } else {
                log.warn("string \'{}\' not contains \'}]\'", elem);
            }
        }

        List<String> filesUrl = new ArrayList<>();
        List<String> filesName = new ArrayList<>();
        for (String elem : strings) {
            String[] subStrings = elem.split("\\}\\,");
            for (String ss : subStrings) {
                String[] needElements = ss.split(",");
                for (String nElem : needElements) {
                    if (nElem.contains("url")) {
                        filesUrl.add(nElem
                                .substring(nElem.indexOf(":") + 2)
                                .replace("\"", "")
                                .replace("\'", "")
                                .trim());
                    }
                    if (nElem.contains("title")) {
                        filesName.add(nElem
                                .substring(nElem.indexOf(":") + 2)
                                .replace("\"", "")
                                .replace("\'", "")
                                .trim());
                    }
                }
            }
        }
        if (filesUrl.size() != filesName.size()) {
            log.debug("size of url {} <> size of fileNames {}: ",
                    filesUrl.size(), filesName.size());
            return null;
        }
        HashMap<String, String> lightFiles = new HashMap<>();
        try {
            for (int i = 0; i < filesUrl.size(); i++) {
                lightFiles.put(filesName.get(i), filesUrl.get(i));
            }
        } catch (Exception ex) {
            if (BuildConfig.DEBUG) {
                ex.printStackTrace();
            } else {
                log.error(Utils.getErrorLogHeader() + new Object() {
                }.getClass().getEnclosingMethod().getName(), ex);
            }
        }
        return lightFiles;
    }

    private static FilmDetails parseArtistDetails(Document doc) {
        FilmDetails filmDetailsObj = new FilmDetails();
        try {
            Element filmDetails = doc.select("table").select("td#body_element")
                    .select("td[valign=top]").first();
            String sFilmTitle = filmDetails.select("div.title").text();
            String sFilmDetails = filmDetails.select("div#content_page").toString();

            String sFilmReviews = doc.select("a[href*=/view_comments/]").text();
            if (sFilmReviews.contains(":")) {
                sFilmReviews = sFilmReviews.substring(sFilmReviews.indexOf(":") + 2);
            } else {
                log.warn("string \'{}\' not contains \':\'", sFilmReviews);
            }
            sFilmReviews = (sFilmReviews.equalsIgnoreCase("")) ?
                    strReviewsDefaultValue : sFilmReviews;

            Elements sFiles = doc.select("table.list")
                    .select("a[href*=/get/]:not(.fox-ic_file_play-btn):not(.fox-play-btn)");
            sFiles = sFiles.select(":not(a[title~=(?i)\\.(png|jpe?g|bmp|gif)])");
            //noinspection MismatchedQueryAndUpdateOfCollection
            List<FilmFile> filesList = new ArrayList<>();
            for (int i = 0; i < sFiles.size(); i++) {
                String sFileUrl = sFiles.get(i).select("a").attr("href");
                String sFileName = sFiles.get(i).select("a").text();
                filesList.add(new FilmFile(sFileName, sFileUrl));
            }

            filmDetailsObj.setFilmTitle(sFilmTitle);
            filmDetailsObj.setFilmReviews(sFilmReviews);
            filmDetailsObj.setFilmDetailsHTML(sFilmDetails);
            filmDetailsObj.setFilmFiles(filesList);
        } catch (IndexOutOfBoundsException ex) {
            if (BuildConfig.DEBUG) {
                ex.printStackTrace();
            } else {
                log.error(Utils.getErrorLogHeader() + new Object() {
                }.getClass().getEnclosingMethod().getName(), ex);
            }
        }
        return filmDetailsObj;
    }

    private static Document openHTMLDocument(String url) {
        InputStream input;
        Document doc = null;
        String fileName;
        if (url.contains("video")) {
            fileName = "sections.html";
            if (url.contains("&per=")) {
                fileName = "films.html";
            }
        } else {
            if (url.equalsIgnoreCase("files_light")) {
                fileName = "filesLight.html";
            } else {
                fileName = "filmDetails.html";
            }
        }
        log.debug("openHTMLdocument: url={}, fname={}", url, fileName);
        //getAssetsList(mContext);
        try {
            input = mContext.getAssets().open(fileName);
            doc = Jsoup.parse(Utils.convertInputStreamToString(input));
        } catch (IOException ex) {
            if (BuildConfig.DEBUG) {
                ex.printStackTrace();
            } else {
                log.error(Utils.getErrorLogHeader() + new Object() {
                }.getClass().getEnclosingMethod().getName(), ex);
            }
        }

        return doc;
    }

    public static void checkForUpdate(final Context context, LoadListener listener) {
        String url = mAppUpdateUrl;
        log.info("checkForUpdate {}", url);
        BobFilmParser.checkNewVersion(url, listener);

    }

    private static void checkNewVersion(final String urlString, final LoadListener listener) {
        mExecService.execute(new Runnable() {
            @Override
            public void run() {
                long fileSize = 0;

                final OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder()
                        .url(mAppUpdateUrl)
                        .build();
                Response response = null;
                try {
                    response = client.newCall(request).execute();
                    fileSize = response.body().contentLength();
                    InputStream input = response.body().byteStream();
                    String result = Utils.convertInputStreamToString(input);
                    AppUpdate appUpdate = Utils.getAppUpdate(result);
                    log.info("getAppUpdate {}", appUpdate);
                    response.body().close();
                    if (appUpdate == null) {
                        throw new InterruptedException("no new version");
                    }
                    listener.OnLoadComplete(appUpdate);
                } catch (Exception ex) {
                    listener.OnLoadError(ex);
                }
            }
        });
    }


    public static void ___checkNewVersion(final String urlString, final LoadListener listener) {
//        mExecService.execute(new Runnable() {
//            @Override
//            public void run() {
        int fileSize = 0;
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.connect();
            fileSize = connection.getContentLength();

            InputStream input = new BufferedInputStream(url.openStream(),
                    8192);

            String result = Utils.convertInputStreamToString(input);

            AppUpdate appUpdate = Utils.getAppUpdate(result);
            log.info("getAppUpdate {}", appUpdate);
            if (appUpdate == null) {
                throw new IOException("no new version");
            }
            input.close();
            connection.disconnect();

            //listener.OnLoadComplete(appUpdate);
        } catch (Exception e) {
            if (BuildConfig.DEBUG) {
                e.printStackTrace();
            }
            //listener.OnLoadError(e);
        }
//            }
//        });
    }

    //this work fine by not need now
    public static void downloadFile(final String urlString, final LoadListener listener) {
        mExecService.execute(new Runnable() {
            @Override
            public void run() {
                int fileSize = 0;
                try {
                    URL url = new URL(urlString);
                    HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    connection.setConnectTimeout(5000);
                    connection.connect();
                    fileSize = connection.getContentLength();

                    // download the file
                    InputStream input = new BufferedInputStream(url.openStream(),
                            8192);
                    String mDownloadDirectory = mContext
                            .getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).toString();
                    String fileName = "update_toseex.apk";
                    log.info("Path {}, FilmFile {}", mDownloadDirectory, fileName);
                    OutputStream output = new FileOutputStream(mDownloadDirectory + "/" + fileName);

                    byte data[] = new byte[4096];
                    long total = 0;
                    int count;
                    while ((count = input.read(data)) != -1) {
                        total += count;
                        output.write(data, 0, count);
                    }

                    output.flush();
                    output.close();
                    input.close();
                    connection.disconnect();
                    log.info("Download finish");
                    listener.OnLoadComplete(mDownloadDirectory + "/" + fileName);
                } catch (Exception e) {
                    if (BuildConfig.DEBUG) {
                        e.printStackTrace();
                    }
                    listener.OnLoadError(e);
                }
            }
        });
    }

    private static boolean checkSubCategories(Document doc) {
        Elements include = doc.select("td#body_element").select("table[class^=include]");
        return include.size() > 0;
    }

    private static JsonObject getDataFromServer(String url) throws Exception {

        BufferedReader connectionBufferedReader = openGetConnection(url);

        JsonObject jRequest = new Gson().fromJson(connectionBufferedReader, JsonObject.class);
        connectionBufferedReader.close();
        return jRequest;
    }

    private static int mConnectionTry = 3;

    private static BufferedReader openGetConnection(String link) throws Exception {
        // should be a singleton
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .writeTimeout(5, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build();
        Request request = new Request.Builder()
                .header("Accept-Charset", "UTF-8")
                .header("Content-Type", "application/json")
                .url(link)
                .build();
        // Get a handler that can be used to post to the main thread
        Response response = client.newCall(request).execute();

        if (!response.isSuccessful()) {
            if (mConnectionTry <= 0) {
                mConnectionTry = 3;
                throw new Exception("URL: " + link + ", HTTP_RESPONSE_CODE: " + response.code());
            } else {
                log.info("try number {}", 3 - mConnectionTry);
                mConnectionTry--;
                openGetConnection(link);
            }
        }
        return new BufferedReader(new InputStreamReader(response.body().byteStream()));
    }

    public interface LoadListener {
        void OnLoadComplete(Object result);

        void OnLoadError(Exception ex);

        //void OnConnectionProblem(Object message);
    }

}