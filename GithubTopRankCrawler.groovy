package com.github.blindpirate.gogradle.statistic

import groovy.json.JsonOutput
import groovy.json.JsonSlurper

import java.nio.file.Path
import java.nio.file.Paths

class GithubTopRankCrawler {
    static String TARGET_URL = 'https://api.github.com/search/repositories?q=stars%3A%3E1+language%3AGo&sort=stars&order=desc&type=Repositories&page=${page}&per_page=100'
    static String TOP_JSON = 'top.json'

    static void main(String[] args) {
        if (args.length == 0) {
            println 'a location must be specified!'
            return
        }
        Path baseDir = Paths.get(args[0])
        getTop1000(baseDir).each {
            cloneOne(baseDir, it.full_name, it.clone_url)
        }
    }

    static List getTop1000(Path baseDir) {
        File topDotJson = baseDir.resolve(TOP_JSON).toFile()
        if (topDotJson.exists()) {
            return new JsonSlurper().parseText(topDotJson.getText())
        } else {
            List allItems = (1..10).collect({ it ->
                String json = new URL(TARGET_URL.replace('${page}', it.toString())).getText()
                return new JsonSlurper().parseText(json).items
            }).flatten()
            topDotJson.write(JsonOutput.toJson(allItems))
            return allItems
        }
    }

    // a/b  https://github.com/a/b.git
    static void cloneOne(Path baseDir, String fullName, String cloneUrl) {
        Path location = baseDir.resolve(fullName.replaceAll(/\//, '_'))
        if (location.toFile().exists()) {
            println("${fullName} exists, skip.")
            return
        }
        runInheritIO(['git', 'clone', cloneUrl, location.toAbsolutePath().toString()], [:])
    }

    static void runInheritIO(List<String> args, Map<String, String> envs) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder().command(args).inheritIO()
        pb.environment().putAll(envs)
        pb.start().waitFor()
    }

}

