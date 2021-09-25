package com.ap.greenpole.stockbroker.utils;

import java.util.List;

/**
 * Created by Oyindamola Akindele on 20/08/2020.
 */
public interface IFileGenerator<T> extends AutoCloseable{

    void open();
    void write(List<T> list);
    void createDirectory(String filename);
}
