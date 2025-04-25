package org.example.Logging.core;

import com.google.errorprone.annotations.CheckReturnValue;

public interface CliLogger {

    void debug(String message);

    void info(String message);

    void warn(String message);

    void error(String message);

    void error(String message, Throwable t);

    @CheckReturnValue
    EntryBuilder debugEntry();

    @CheckReturnValue
    EntryBuilder infoEntry();

    @CheckReturnValue
    EntryBuilder warnEntry();

    @CheckReturnValue
    EntryBuilder errorEntry();

    interface EntryBuilder {
        @CheckReturnValue
        EntryBuilder message(String message);

        @CheckReturnValue
        EntryBuilder field(String key, Object value);

        @CheckReturnValue
        EntryBuilder exception(Throwable t);

        @CheckReturnValue
        EntryBuilder command(String... args);

        void log();

    }
}