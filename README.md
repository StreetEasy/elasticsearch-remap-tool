Elasticsearch Utility
=====================

The Elasticsearch Utility enables on-the-fly remappings with no service
downtime. This is achieved by pushing content from an existing index into a new
index (to which the new mappings have been applied).

Build
-----

This is an sbt project. From the es-utils directory, type sbt dist to create a
target/es_utils.jar file

Remapping
---------

Note, perform a basic sanity check on the data after steps (1) and (3) before
moving on.

Also note, from step 2 onwards, the live API will not pick up new content until
step 4 is complete. So be as quick as possible!

1. Run the `remap` command
2. Use the `update-alias` command to move the 'content-api-write' alias to the new index
3. Reindex all for the period since the remapping began (generally the last day)
4. Use `update-alias` to move the content-api-read alias to the new index

Commands in detail
------------------

To run any command, ssh onto the box. Then:

    $ cd /home/content-api/utils
    $ java -jar es-utils.jar [cmd with args]

### remap

    remap sourceIdx targetIdx [mappingsFile] [batchSize] [writeTimeOutMillis]

The batchSize parameter determines the number of documents updated at a time. It
defaults to 500 and will not accept a value lower than 10.

The writeTimeOutMillis determines the timeout for index actions against the new
index. It defaults to 30000 (with a minimum value of 1000). There is no timeout
applied to reads from the existing index.

Note: You should run remaps via 'screen' so that you can detach from the
terminal if needed. The PROD index can take 3+ hours to complete.

### update-alias

    update-alias sourceIdx targetIdx alias

Potential pitfalls
------------------

Make sure that your source index, target index, and alias all exist.

If you provide your own mappings.json be careful to make sure it is correct. You
should probably never pass in a mappings file when remapping on PROD. Instead
update the default_mapping.json file in a PR and merge it in before performing a
remap. Elasticsearch will then apply this mapping automatically to any new index
at creation time.

Lastly, make sure any alias used for insert operations (such as
`content-api-write`) only points to a single index at a time; ES will not
process insert operations against an alias with multiple indices.

