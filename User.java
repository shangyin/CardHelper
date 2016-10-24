package CardHelper;

import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

import java.io.File;
import java.io.FileWriter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by 41237 on 2016/10/22.
 */
public class User
{
    private String name;
    private String password;
    private String email;
    private LocalDate localDate;

    public String getName()
    {
        return name;
    }

    public String getPassword()
    {
        return password;
    }

    public String getEmail()
    {
        return email;
    }

    public LocalDate getLocalDate() {
        return localDate;
    }

    public User(String name, String password, String email, String date)
    {
        this.name = name;
        this.password = password;
        this.email = email;
        if (date.equals("")) {
            this.localDate = LocalDate.now();
        } else {
            this.localDate = LocalDate.parse(date, DateTimeFormatter.BASIC_ISO_DATE);
        }
    }

    @Override
    public String toString()
    {
        return "name:" + name + "\tpassword: " + password + "\temail: " + email;
    }

    public static List<User> parseUsers(String file) throws Exception
    {
        File xml = new File(file);
        SAXReader reader = new SAXReader();
        org.dom4j.Document doc = reader.read(xml);
        List<Element> elements = doc.getRootElement().elements();

        return elements.stream()
                .map(x -> new User(x.element("name").getText(),
                        x.element("password").getText(),
                        x.element("email").getText(),
                        x.element("date").getText()))
                .collect(Collectors.toList());
    }

    public static void saveUser(String file, User user) throws Exception
    {
        SAXReader sr = new SAXReader();
        org.dom4j.Document doc = sr.read(new File(file));

        Element element = doc.getRootElement().addElement("user");
        element.addElement("name").setText(user.name);
        element.addElement("password").setText(user.password);
        element.addElement("email").setText(user.email);
        element.addElement("date").setText(user.localDate.format(DateTimeFormatter.BASIC_ISO_DATE));

        OutputFormat format = OutputFormat.createPrettyPrint();
        format.setEncoding("utf-8");
        XMLWriter writer = new XMLWriter(new FileWriter(new File(file)), format);
        writer.write(doc);
        writer.close();
    }
}
