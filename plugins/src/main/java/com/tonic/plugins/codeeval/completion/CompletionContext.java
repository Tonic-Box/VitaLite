package com.tonic.plugins.codeeval.completion;

import com.tonic.plugins.codeeval.SimpleCodeEvaluator;
import com.tonic.util.ThreadPool;
import lombok.Getter;

import java.util.Set;

/**
 * Encapsulates all classloader-dependent components for a single completion/evaluation context.
 * Each context has its own ClassCache, TypeInference, CompletionProvider, and Evaluator.
 */
@Getter
public class CompletionContext {
    private final String name;
    private final ClassLoader classLoader;
    private final ClassCache classCache;
    private final TypeInference typeInference;
    private final VitaCompletionProvider completionProvider;
    private final SimpleCodeEvaluator evaluator;
    private volatile boolean initialized = false;

    public CompletionContext(String name, ClassLoader classLoader, ImportScanner sharedImportScanner) {
        this.name = name;
        this.classLoader = classLoader;
        this.classCache = new ClassCache(classLoader);
        this.typeInference = new TypeInference(classCache, sharedImportScanner);
        this.completionProvider = new VitaCompletionProvider(classCache, typeInference, sharedImportScanner);
        this.evaluator = new SimpleCodeEvaluator(classLoader);
    }

    /**
     * Initializes the context asynchronously by indexing packages.
     * @param packagesToIndex Set of package names to index
     * @param onComplete Callback to run when initialization is complete (may be null)
     */
    public void initializeAsync(Set<String> packagesToIndex, Runnable onComplete) {
        ThreadPool.submit(() -> {
            classCache.indexPackages(packagesToIndex);
            typeInference.rebuildContext();
            initialized = true;
            if (onComplete != null) {
                onComplete.run();
            }
        });
    }

    /**
     * Index root package classes (obfuscated client classes with no package).
     * Should be called after initializeAsync completes for Client context.
     */
    public void indexRootPackageClasses() {
        classCache.indexRootPackageClasses();
    }
}
