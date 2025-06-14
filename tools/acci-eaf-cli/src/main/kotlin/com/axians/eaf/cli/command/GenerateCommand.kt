package com.axians.eaf.cli.command

import picocli.CommandLine

@CommandLine.Command(
    name = "generate",
    description = ["Generate EAF components and services"],
    mixinStandardHelpOptions = true,
    subcommands = [
        GenerateServiceCommand::class,
        GenerateAggregateCommand::class,
        GenerateCommandCommand::class,
        GenerateEventCommand::class,
        GenerateProjectorCommand::class,
    ],
)
class GenerateCommand : Runnable {
    override fun run() {
        println("Generate command")
        println("Use --help to see available generation options")
    }
}
