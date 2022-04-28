import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * To run set sonar.core to provided in pom.xml
 * 
 * @author eho
 */
public class PropertiesUpdater {

  private static final String EN_PROPS = "org/sonar/l10n/core.properties";
  private static final String DE_PROPS = "org/sonar/l10n/core_de.properties";

  public static void main(String[] args) throws Exception {
    final Properties coreEN = new Properties();
    try (Reader reader = new BufferedReader(
        new InputStreamReader(PropertiesUpdater.class.getResourceAsStream(EN_PROPS), UTF_8))) {

      coreEN.load(reader);
    }

    final Properties coreDE = new Properties();
    try (Reader reader = new BufferedReader(
        new InputStreamReader(PropertiesUpdater.class.getResourceAsStream(DE_PROPS), UTF_8))) {

      coreDE.load(reader);
    }

    for (String arg : args) {
      if (!Files.isReadable(Paths.get(arg))) {
        continue;
      }
      try (Reader reader = new BufferedReader(new FileReader(arg, UTF_8))) {
        coreDE.load(reader);
      }
    }

    coreDE.keySet().stream().sorted().filter(name -> !coreEN.containsKey(name))
        .forEach(name -> System.out.println("Unused property: " + name));

    final List<String> missingProps = coreEN.keySet().stream().map(Object::toString).sorted()
        .filter(name -> !coreDE.containsKey(name)).collect(Collectors.toList());
    missingProps.forEach(name -> System.out.println("Missing property: " + name));

    coreEN.keySet().forEach(name -> coreDE.putIfAbsent(name, coreEN.get(name)));

    final StringBuilder fileContent = new StringBuilder();

    try (final BufferedReader reader = new BufferedReader(
        new InputStreamReader(PropertiesUpdater.class.getResourceAsStream(EN_PROPS)))) {

      String line;
      while ((line = reader.readLine()) != null) {
        if (!line.stripLeading().startsWith("#") && line.contains("=")) {
          Properties tmp = new Properties();
          try (StringReader sr = new StringReader(line)) {
            tmp.load(sr);
          }
          String key = tmp.propertyNames().nextElement().toString();
          fileContent.append(key.replace("=", "\\="));
          fileContent.append("=");
          fileContent.append(coreDE.get(key));
        } else {
          fileContent.append(line);
        }
        fileContent.append('\n');
      }
    }

    try (FileOutputStream fos = new FileOutputStream("core_de.properties.new")) {
      fos.write(fileContent.toString().getBytes(UTF_8));
    }

    try (FileWriter writer = new FileWriter("core_de.properties.missing", UTF_8)) {
      for (String name : missingProps) {
        writer.append(name.replace("=", "\\=") + "=" + coreEN.get(name));
        writer.append(System.lineSeparator());
      }
    }
  }
}
