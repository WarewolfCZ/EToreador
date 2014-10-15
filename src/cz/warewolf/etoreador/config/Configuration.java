package cz.warewolf.etoreador.config;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

/**
 * Loading configuration from .properties file 
 * @author Denis
 */
public class Configuration {

    private String configPath;
    private Properties prop;

    public Configuration(String path) {
        this.configPath = path;
        this.prop = new Properties();
    }

    /**
     * Load configuration from file
     * 
     * @return true if loaded
     */
    public boolean loadConfig() {

        boolean result = false;
        InputStream inputStream;
        try {
            inputStream = new FileInputStream(configPath);
            prop.load(inputStream);
            result = true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * Get configuration value
     * 
     * @param key
     * @return Null if value doesn't exist
     */
    public String getValue(String key) {
        return this.prop.getProperty(key);
    }

    public String getValue(String key, String defaultValue) {
        return this.prop.getProperty(key, defaultValue);
    }
    
    /**
     *  Get all configuration keys
     * @return List<String> list of all keys
     */
    public List<String> getKeys() {
        List<String> result = new ArrayList<String>();
        Enumeration<Object> keys = this.prop.keys();
        while (keys.hasMoreElements()) {
            result.add((String) keys.nextElement());
        }
        return result;
    }
}