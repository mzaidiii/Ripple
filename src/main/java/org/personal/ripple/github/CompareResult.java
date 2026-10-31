package org.personal.ripple.github;

import java.util.List;

public record CompareResult(List<CommitItem> commits , List<FileItem> files) {
}
