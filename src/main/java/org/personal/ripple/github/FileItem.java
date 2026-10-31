package org.personal.ripple.github;

public record FileItem(String filename , String status , int additions , int deletions , String patch) {
}
