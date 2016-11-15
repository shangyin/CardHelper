package CardHelper;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by 41237 on 2016/10/21.
 */
public class XinXiMenHu
{
    private static AtomicInteger count = new AtomicInteger(0);
    private final String picFile;


    CloseableHttpClient client = HttpClients.createDefault();
    final String user;
    final String password;

    public XinXiMenHu(String user, String psw) throws Exception
    {
        int num = count.incrementAndGet();
        if (System.getProperty("os.name").contains("Windows")) {
            picFile = "d:\\test" + num + ".jpg";
        } else {
            picFile = "/root/lib/test" + num + ".jpg";
        }
        this.user = user;
        this.password = psw;

        String res;
        getCookie();
        do {
            getPic();
            res = Utils.parsePic(picFile);
        } while (!verifyPic(res));

        if(!login(user, psw, res)) {
            throw new Exception("error info");
        }
    }

    private void getCookie() throws IOException
    {
        client.execute(new HttpGet("http://my.gdufe.edu.cn/")).close();
    }

    private boolean login(String user, String psw, String res) throws IOException
    {
        HttpPost post = new HttpPost("http://my.gdufe.edu.cn/userPasswordValidate.portal");
        List<NameValuePair> args = new ArrayList<>();
        args.add(new BasicNameValuePair("Login.Token1", user));
        args.add(new BasicNameValuePair("Login.Token2", psw));
        args.add(new BasicNameValuePair("captchaField", res));
        args.add(new BasicNameValuePair("goto", "http://my.gdufe.edu.cn/loginSuccess.portal"));
        args.add(new BasicNameValuePair("gotoOnFail", "http://my.gdufe.edu.cn/loginFailure.portal"));
        post.setEntity(new UrlEncodedFormEntity(args));
        CloseableHttpResponse response = client.execute(post);

        String html = Utils.getContent(response);
        response.close();
        return html.contains("Successed") ;
    }

    private boolean verifyPic(String res) throws IOException
    {
        //validate img GET
        String param = "captcha=" + res + "&" + "what=captcha&value=" + res + "&_=";
        HttpGet get = new HttpGet("http://my.gdufe.edu.cn/captchaValidate.portal?" + param);
        CloseableHttpResponse response = client.execute(get);
        response.close();
        //验证码请求如果正确，Content-Length为0, 否则为15
        return response.getHeaders("Content-Length")[0]
                .getValue()
                .equals("0");
    }

    private int getPic() throws IOException
    {
        //get pic GET
        int count = new Random(System.currentTimeMillis()).nextInt(5);
        HttpGet get = new HttpGet("http://my.gdufe.edu.cn/captchaGenerate.portal?s=" + count);
        CloseableHttpResponse response = client.execute(get);

        //save pic
        BufferedInputStream from = new BufferedInputStream(response.getEntity().getContent());
        BufferedOutputStream to = new BufferedOutputStream(new FileOutputStream(picFile));
        to.write(from.read());
        while (from.available() > 0)
        {
            to.write(from.read());
        }
        from.close();
        to.close();
        response.close();
        return count;
    }

    public void close() throws Exception
    {
        client.close();
    }
}

