package com.buschmais.jqassistant.plugin.yaml.impl.scanner;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import com.buschmais.jqassistant.core.scanner.api.Scanner;
import com.buschmais.jqassistant.core.scanner.api.ScannerContext;
import com.buschmais.jqassistant.core.scanner.api.ScannerPlugin.Requires;
import com.buschmais.jqassistant.core.scanner.api.Scope;
import com.buschmais.jqassistant.core.store.api.Store;
import com.buschmais.jqassistant.plugin.common.api.model.FileDescriptor;
import com.buschmais.jqassistant.plugin.common.api.scanner.AbstractScannerPlugin;
import com.buschmais.jqassistant.plugin.common.api.scanner.filesystem.FileResource;
import com.buschmais.jqassistant.plugin.yaml.api.model.YAMLDocumentDescriptor;
import com.buschmais.jqassistant.plugin.yaml.api.model.YAMLFileDescriptor;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;
import org.yaml.snakeyaml.resolver.Resolver;
import org.yaml.snakeyaml.serializer.Serializer;

@Requires(FileDescriptor.class)
public class YAMLFileScannerPlugin extends AbstractScannerPlugin<FileResource, YAMLFileDescriptor> {

    /**
     * Supported file extensions for YAML file resources.
     */
    public final static String YAML_FILE_EXTENSION = ".yaml";
    public final static String YML_FILE_EXTENSION = ".yml";

    @Override
    public boolean accepts(FileResource file, String path, Scope scope) {
        String lowercasePath = path.toLowerCase();
        return lowercasePath.endsWith(YAML_FILE_EXTENSION) || lowercasePath.endsWith(YML_FILE_EXTENSION);
    }

    @Override
    public YAMLFileDescriptor scan(FileResource item, String path, Scope scope, Scanner scanner) throws IOException {
        ScannerContext context = scanner.getContext();
        Store store = context.getStore();

        Yaml yaml = new Yaml(new TagOverridingConstructor(), new Representer(), new DumperOptions(),
                             new NonResolvingResolver());
        Representer representer = new Representer();
        DumperOptions options = new DumperOptions();

        FileDescriptor fileDescriptor = context.getCurrentDescriptor();
        YAMLFileDescriptor yamlFileDescriptor = store.addDescriptorType(fileDescriptor, YAMLFileDescriptor.class);


        try (InputStream in = item.createStream()) {
            Iterable<Object> docs = yaml.loadAll(in);

            for (Object doc : docs) {
                Node node = representer.represent(doc);
                YAMLEmitter emitter = new YAMLEmitter(yamlFileDescriptor, scanner);
                Serializer serializer = new Serializer(emitter, new Resolver(), options, null);

                serializer.open();
                serializer.serialize(node);
                serializer.close();
            }
            // In case the content of the file is not parseable set parsed=false
            // to help the user to identify nonparseable files
            yamlFileDescriptor.setValid(true);
        } catch (RuntimeException rt) {
            yamlFileDescriptor.setValid(false);
            for (YAMLDocumentDescriptor documentDescriptor : yamlFileDescriptor.getDocuments()) {
                yamlFileDescriptor.getDocuments().remove(documentDescriptor);
            }
            // @todo Logging is desired here Oliver B. Fischer, 23.08.2015
        }

        return yamlFileDescriptor;
    }

    /**
     * Non-resolving resolver to avoid automatic type conversion provided by
     * the used SnakeYAML libary.
     *
     * One good example for this disabled automatic type coversion is the
     * conversion of the string `OFF` to the boolean value `false`
     */
    private static class NonResolvingResolver extends Resolver {
        @Override
        protected void addImplicitResolvers() {
        }
    }


    class TagOverridingConstructor extends Constructor {
        private List<Tag> SUPPORTED_TAGS =
             Arrays.asList(Tag.YAML, Tag.MERGE,
                           Tag.SET, Tag.PAIRS, Tag.OMAP,
                           Tag.BINARY, Tag.INT, Tag.FLOAT,
                           Tag.BOOL, Tag.NULL,
                           Tag.STR, Tag.SEQ, Tag.MAP);

        @Override
        protected Object constructObject(Node node) {
            Tag tag = node.getTag();

            if (!SUPPORTED_TAGS.contains(tag)) {
                node.setTag(Tag.STR);
            }

            return super.constructObject(node);
        }


    }
}
