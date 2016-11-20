package CardHelper;

import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.time.LocalTime;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Created by 41237 on 2016/11/3.
 */
public class BootStrap
{
    private static ContentUtil contentUtil;

    public static void main(String[] args) throws Exception
    {
        //获取发送发的信息和ps信息
        Properties pro = new Properties();
        Reader reader = new InputStreamReader(new FileInputStream(args[0]), "utf-8");
        pro.load(reader);
        SendMail host = new SendMail(pro.getProperty("email"), pro.getProperty("password"));
        contentUtil = new ContentUtil(args[1], pro.getProperty("ps"));
        User.upassword = pro.getProperty("upassword");
        User.user = pro.getProperty("user");

        //根据发送类型选择用户
        //daily需要按时间发（20点之前之后）
        //monthly和weekly为所有用户
        LocalTime from;
        LocalTime to;
        if (args[1].equals("daily")) {
            from = LocalTime.now().withMinute(0).withSecond(0).withNano(0);
            to = from.plusHours(1).minusMinutes(1);
        } else {
            from = LocalTime.MIN;
            to = LocalTime.MAX;
        }
       List<User> users = User.researchUsers(from, to);
//        List<User> users = User.parseUsers("user.xml");


        //多线程处理每个用户
        ExecutorService executor = Executors.newFixedThreadPool(10);
        List<CompletableFuture<String>> recordsFutures = users.stream()
                .map(u -> CompletableFuture.supplyAsync(() -> send(host, u), executor))
                .collect(Collectors.toList());
        recordsFutures.stream()
                .map(CompletableFuture::join)
                .forEach(System.out::println);
        executor.shutdown();
    }


    static String send(SendMail mail, User user) {
        try {
            XinXiMenHu xinXiMenHu = new XinXiMenHu(user.getName(), user.getPassword());
            CardCenter cardCenter = new CardCenter(xinXiMenHu);
            List<Record> records = cardCenter.getRecords(contentUtil.fromStr, contentUtil.toStr);
            mail.send(user.getEmail(), contentUtil.getTitle(records), contentUtil.getContent(records));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "finish user " + user;
    }
}
