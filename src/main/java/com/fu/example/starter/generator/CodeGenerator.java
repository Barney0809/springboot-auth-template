package com.fu.example.starter.generator;

import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.OutputFile;
import com.baomidou.mybatisplus.generator.engine.FreemarkerTemplateEngine;
import com.baomidou.mybatisplus.generator.model.ClassAnnotationAttributes;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collections;

public class CodeGenerator {

    public static final String URL = "jdbc:mysql://localhost:3306/starter_db?serverTimezone=Asia/Shanghai&useUnicode=true&characterEncoding=utf-8&allowMultiQueries=true&useSSL=false";
    public static final String USER_NAME = "root";
    public static final String PASSWORD = "123456";

    public static final String TABLE_NAME = "user";
    public static void main(String[] args) {
        FastAutoGenerator.create(URL, USER_NAME, PASSWORD)
                .globalConfig(builder -> {
                    builder.author("FU") // 设置作者
                            .outputDir("src/main/java")
                            .commentDate("yyyy-MM-dd")
                            .disableOpenDir()
                            .enableSpringdoc()
                    ; // 指定输出目录
                })
                .packageConfig(builder ->
                        builder.parent("com.fu.example.starter") // 设置父包名
//                                .moduleName("generate") // 设置父包模块名
                                .pathInfo(Collections.singletonMap(OutputFile.xml, "src/main/resources/mapper")) // 设置mapperXml生成路径
                                .entity("model.entity")
                )

                .strategyConfig(builder ->
                        builder.addInclude(TABLE_NAME) // 设置需要生成的表名
                                .entityBuilder()
                                .enableFileOverride()
                                .enableSerialAnnotation()
//                                .enableLombok()
                                .enableLombok(new ClassAnnotationAttributes("@Data","lombok.Data"))
                                .enableTableFieldAnnotation()

                                .controllerBuilder()
                                .enableRestStyle()
                                .enableFileOverride()

                                .mapperBuilder()
                                .enableFileOverride()
                                .enableBaseColumnList()
                                .enableBaseResultMap()
                                .mapperAnnotation(Mapper.class)

                                .serviceBuilder()
                                .enableFileOverride()
                                .formatServiceImplFileName("%sServiceImpl")
                                .formatServiceFileName("%sService")

                )
                .templateEngine(new FreemarkerTemplateEngine()) // 使用Freemarker引擎模板，默认的是Velocity引擎模板
                .execute();
    }
}
