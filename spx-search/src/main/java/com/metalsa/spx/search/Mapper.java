package com.metalsa.spx.search;

/**
 * Created by IntelliJ Idea
 *
 * @author ruben.bresler
 */
public interface Mapper<F, T> {
    T convert(F from);
}
