package com.axians.eaf.cli

import com.axians.eaf.cli.command.GenerateCommand
import picocli.CommandLine
import kotlin.system.exitProcess

@CommandLine.Command(
    name = "eaf",
    description = ["ACCI EAF CLI - Command line tool for generating EAF services and components"],
    mixinStandardHelpOptions = true,
    version = ["ACCI EAF CLI v0.1.0"],
    subcommands = [GenerateCommand::class],
)
class AcciEafCliApplication : Runnable {
    override fun run() {
        println("ACCI EAF CLI")
        println("Use --help to see available commands")
    }
}

fun main(args: Array<String>) {
    val exitCode = CommandLine(AcciEafCliApplication()).execute(*args)
    exitProcess(exitCode)
}
