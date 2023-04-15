# 1. 入口：ClassPathXmlApplicationContext
```java
public class Main {
    public static void main(String[] args) {
        ApplicationContext applicationContext = new ClassPathXmlApplicationContext("classpath:spring-beans.xml");
        Student student = applicationContext.getBean("student", Student.class);
    }
}
```

```java
public class ClassPathXmlApplicationContext extends AbstractXmlApplicationContext {
    public ClassPathXmlApplicationContext(String configLocation) throws BeansException {
        this(new String[]{configLocation}, true, null);
    }

    public ClassPathXmlApplicationContext(
            String[] configLocations, boolean refresh, @Nullable ApplicationContext parent)
            throws BeansException {

        super(parent);
        setConfigLocations(configLocations);
        if (refresh) {
            refresh();
        }
    }
}
```

## 1.1 super(parent)

如图所示，一级一级往上访问直到AbstractApplicationContext类构造方法,里面做了两件事, 实际只初始化了resourcePatternResolver。
1. 初始化AbstractApplicationContext的resourcePatternResolver = new PathMatchingResourcePatternResolver(this);
2. 设置父容器。由于parent为null，所以这里没有执行。   

![体系结构.png](../../image/体系结构.png)

```java
public abstract class AbstractApplicationContext extends DefaultResourceLoader
        implements ConfigurableApplicationContext {

    private ResourcePatternResolver resourcePatternResolver;

    private ApplicationContext parent;

    public AbstractApplicationContext(@Nullable ApplicationContext parent) {
        this();
        setParent(parent);
    }

    //this()
    public AbstractApplicationContext() {
        this.resourcePatternResolver = getResourcePatternResolver();
    }

    protected ResourcePatternResolver getResourcePatternResolver() {
        return new PathMatchingResourcePatternResolver(this);
    }

    public void setParent(@Nullable ApplicationContext parent) {
        this.parent = parent;
        if (parent != null) {
            //由于parent为null，所以这里的代码不执行
            Environment parentEnvironment = parent.getEnvironment();
            if (parentEnvironment instanceof ConfigurableEnvironment) {
                getEnvironment().merge((ConfigurableEnvironment) parentEnvironment);
            }
        }
    }
}
```

## 1.2 setConfigLocations(configLocations)
该方法继承自父类AbstractRefreshableConfigApplicationContext，用来将bean的配置文件路径 处理完$引用 后设置给configLocations数组。
处理逻辑为：递归解析包含${}的文本，然后从AbstractApplicationContext的属性MutablePropertySources(java环境变量、系统环境变量)中获取值替换占位符
```java
public abstract class AbstractRefreshableConfigApplicationContext extends AbstractRefreshableApplicationContext
        implements BeanNameAware, InitializingBean {
    
    private String[] configLocations;

    public void setConfigLocations(@Nullable String... locations) {
        if (locations != null) {
            Assert.noNullElements(locations, "Config locations must not be null");
            this.configLocations = new String[locations.length];
            for (int i = 0; i < locations.length; i++) {
                //该方法的主要业务
                this.configLocations[i] = resolvePath(locations[i]).trim();
            }
        } else {
            this.configLocations = null;
        }
    }
}
```

### 1.2.1 resolvePath(path)
![setLocations.png](../../image/setLocations.png)
```java
public abstract class AbstractRefreshableConfigApplicationContext extends AbstractRefreshableApplicationContext
        implements BeanNameAware, InitializingBean {

    protected String resolvePath(String path) {
        /*
         * getEnvironment()返回StandardEnvironment实例，等同于new StandardEnvironment().resolveRequiredPlaceholders(path)
         */
        return getEnvironment().resolveRequiredPlaceholders(path);
    }
}
```
getEnvironment()方法继承自父类AbstractApplicationContext，返回new StandardEnvironment()

由于AbstractEnvironment是StandardEnvironment的父类，因此先被加载并初始化

```java
public class StandardEnvironment extends AbstractEnvironment {
    protected void customizePropertySources(MutablePropertySources propertySources) {
        propertySources.addLast(
                new PropertiesPropertySource(SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME, getSystemProperties()));
        propertySources.addLast(
                new SystemEnvironmentPropertySource(SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, getSystemEnvironment()));
    }
}
```

AbstractEnvironment实例化时会先初始化（propertySources与propertyResolver两个属性）

同时，resolveRequiredPlaceholders(path)方法在StandardEnvironment类中没有，而是继承自父类AbstractEnvironment。

在AbstractEnvironment的resolveRequiredPlaceholders(path)方法中，去调用this.propertyResolver的resolveRequiredPlaceholders(text)
而this.propertyResolver在AbstractEnvironment实例化时就被赋值为PropertySourcesPropertyResolver了，因此这里相当于去调用
PropertySourcesPropertyResolver的resolveRequiredPlaceholders(text)方法

```java
public abstract class AbstractEnvironment implements ConfigurableEnvironment {
    private final MutablePropertySources propertySources;
    
    private final ConfigurablePropertyResolver propertyResolver;

    public AbstractEnvironment() {
        this(new MutablePropertySources());
    }
    
    /**
    * 1. 初始化 this.propertySources = new MutablePropertySources()，且MutablePropertySources的propertySourceList属性中包含systemProperties与systemEnvironment
    * 2。初始化 this.propertyResolver = new PropertySourcesPropertyResolver(propertySources)
    */
    protected AbstractEnvironment(MutablePropertySources propertySources) {
        //this.propertySources = new MutablePropertySources()
        this.propertySources = propertySources;
        //this.propertyResolver = new PropertySourcesPropertyResolver(propertySources)
        this.propertyResolver = createPropertyResolver(propertySources);
        //由子类StandardEnvironment实现该方法
        //子类逻辑：将systemProperties与systemEnvironment两个properties添加到AbstractEnvironment类的变量propertySources的List<PropertySource<?>> propertySourceList中
        customizePropertySources(propertySources);
    }

    protected ConfigurablePropertyResolver createPropertyResolver(MutablePropertySources propertySources) {
        return new PropertySourcesPropertyResolver(propertySources);
    }

    protected void customizePropertySources(MutablePropertySources propertySources) {
        //模版方法，由子类StandardEnvironment实现
    }
    
    public String resolveRequiredPlaceholders(String text) throws IllegalArgumentException {
        // propertyResolver在当前类实例化时就给赋值为PropertySourcesPropertyResolver
        return this.propertyResolver.resolveRequiredPlaceholders(text);
    }

    public ConfigurableEnvironment getEnvironment() {
        if (this.environment == null) {
            this.environment = createEnvironment();
        }
        return this.environment;
    }

    protected ConfigurableEnvironment createEnvironment() {
        return new StandardEnvironment();
    }
}
```

而PropertySourcesPropertyResolver类并没有resolveRequiredPlaceholders方法的实现，而是通过父类AbstractPropertyResolver来实现
这里相当于调用类PropertyPlaceholderHelper类的replacePlaceholders方法。其接收string类型和PlaceholderResolver函数接口类型共两个参数
this::getPropertyAsRawString是使用lambda表达式的方式，将getPropertyAsRawString函数作为PlaceholderResolver函数接口的实现。
```java
public abstract class AbstractPropertyResolver implements ConfigurablePropertyResolver {
    private PropertyPlaceholderHelper strictHelper;
    
    public String resolveRequiredPlaceholders(String text) throws IllegalArgumentException {
        if (this.strictHelper == null) {
            this.strictHelper = createPlaceholderHelper(false);
        }
        return doResolvePlaceholders(text, this.strictHelper);
    }

    private PropertyPlaceholderHelper createPlaceholderHelper(boolean ignoreUnresolvablePlaceholders) {
        return new PropertyPlaceholderHelper(this.placeholderPrefix, this.placeholderSuffix,
                this.valueSeparator, ignoreUnresolvablePlaceholders);
    }
    
    private String doResolvePlaceholders(String text, PropertyPlaceholderHelper helper) {
        return helper.replacePlaceholders(text, this::getPropertyAsRawString);
    }
}
```
```java
public class PropertyPlaceholderHelper {
    public String replacePlaceholders(String value, final Properties properties) {
        Assert.notNull(properties, "'properties' must not be null");
        return replacePlaceholders(value, properties::getProperty);
    }

    public String replacePlaceholders(String value, PlaceholderResolver placeholderResolver) {
        Assert.notNull(value, "'value' must not be null");
        return parseStringValue(value, placeholderResolver, null);
    }

    protected String parseStringValue(
            String value, PlaceholderResolver placeholderResolver, @Nullable Set<String> visitedPlaceholders) {
        //略
    }
    
    @FunctionalInterface
    public interface PlaceholderResolver {

        /**
         * Resolve the supplied placeholder name to the replacement value.
         * @param placeholderName the name of the placeholder to resolve
         * @return the replacement value, or {@code null} if no replacement is to be made
         */
        @Nullable
        String resolvePlaceholder(String placeholderName);
    }
}
```

## 1.3 refresh()
