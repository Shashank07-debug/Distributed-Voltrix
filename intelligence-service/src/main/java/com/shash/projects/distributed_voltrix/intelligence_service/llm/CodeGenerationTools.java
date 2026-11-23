package com.shash.projects.distributed_voltrix.intelligence_service.llm;

import com.shash.projects.distributed_voltrix.intelligence_service.client.WorkspaceClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * One instance is created per request, so the state below (already-read files,
 * call counter) is scoped to a single chat turn.
 */
@Slf4j
public class CodeGenerationTools {

    /** Stay well below any external tool-call limit. */
    private static final int MAX_READ_CALLS = 8;

    private final Long projectId;
    private final WorkspaceClient workspaceClient;

    private final Set<String> alreadyRead = ConcurrentHashMap.newKeySet();
    private final AtomicInteger readCalls = new AtomicInteger();

    public CodeGenerationTools(Long projectId, WorkspaceClient workspaceClient) {
        this.projectId = projectId;
        this.workspaceClient = workspaceClient;
    }

    @Tool(name = "read_files",
            description = "Read the content of files. Only input file names present inside the FILE_TREE. "
                    + "DO NOT input any path which is not present under the FILE_TREE. "
                    + "Each file can be read only ONCE per request. After reading, do NOT call this tool again "
                    + "for the same files; write your final answer instead.")
    public List<String> readFiles(
            @ToolParam(description = "List of relative paths (e.g., ['src/App.tsx'])")
            List<String> paths
    ) {
        int call = readCalls.incrementAndGet();
        if (call > MAX_READ_CALLS) {
            log.warn("read_files call cap reached for projectId {} (call #{})", projectId, call);
            return List.of("LIMIT REACHED: you cannot read any more files. "
                    + "Write your final answer now using only the files you have already read.");
        }

        if (paths == null || paths.isEmpty()) {
            return List.of("ERROR: no paths were provided. Pass a list of paths from the FILE_TREE.");
        }

        List<String> result = new ArrayList<>();

        for (String path : paths) {
            if (path == null || path.isBlank()) {
                continue;
            }

            String cleanPath = path.startsWith("/") ? path.substring(1) : path;

            if (!alreadyRead.add(cleanPath)) {
                log.info("Duplicate read blocked: {}", cleanPath);
                result.add("NOTICE: '" + cleanPath + "' was already read in this request. "
                        + "Use the content you already have and give your final answer now.");
                continue;
            }

            log.info("Requested file: {}", cleanPath);

            try {
                String content = workspaceClient.getFileContent(projectId, cleanPath);

                if (content == null || content.isBlank()) {
                    log.warn("File '{}' is missing or empty for projectId {}", cleanPath, projectId);
                    result.add("ERROR: '" + cleanPath + "' does not exist or is empty. "
                            + "Do not request it again.");
                    continue;
                }

                log.info("Read {} -> {} chars", cleanPath, content.length());
                result.add(String.format(
                        "--- START OF FILE: %s ---\n%s\n--- END OF FILE ---",
                        cleanPath, content
                ));
            } catch (Exception e) {
                log.warn("Failed to read '{}' for projectId {}: {}", cleanPath, projectId, e.getMessage());
                result.add("ERROR: could not read '" + cleanPath + "' (" + e.getMessage() + "). "
                        + "Do not request it again.");
            }
        }

        return result;
    }
}
