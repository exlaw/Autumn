package cn.imaq.autumn.integration.mybatis;

import cn.imaq.autumn.core.beans.BeanInfo;
import cn.imaq.autumn.core.beans.scanner.BeanScanner;
import cn.imaq.autumn.core.context.AutumnContext;
import io.github.lukehutch.fastclasspathscanner.scanner.ScanSpec;
import org.apache.ibatis.annotations.Mapper;

public class MapperScanner implements BeanScanner {
    @Override
    public void process(ScanSpec spec, AutumnContext context) {
        spec.matchClassesWithAnnotation(Mapper.class, cls -> {
            if (cls.isInterface()) {
                String name = cls.getSimpleName().toLowerCase();
                context.addBeanInfo(BeanInfo.builder()
                        .name(name)
                        .type(cls)
                        .singleton(false)
                        .creator(new MapperCreator(cls, context))
                        .build());
            }
        });
    }
}
