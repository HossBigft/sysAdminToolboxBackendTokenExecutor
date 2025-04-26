package org.example;

public sealed interface ServiceCommand permits PleskCommand, NsCommand {}

