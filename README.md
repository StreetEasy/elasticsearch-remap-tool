Elasticsearch Remap Tool
========================

The Elasticsearch Remap Tool enables on-the-fly remappings with little service downtime. This is achieved by pushing content from an existing index into a new index to which the new mappings have been applied.

This process requires your index to have 'write' and 'read' aliases assigned to it which are used by the producers and consumers of the index.


Build
-----

Running `build.sh` will produce a `elasticsearch-remap-tool.jar` in the `target` directory.


Remapping
---------

You should perform a basic sanity check on the data after steps (1) and (3) before moving on.

Note that from step 2 onwards, the index will not receive new documents until step 4 is complete. So be as quick as possible!

1. Run `remap`
2. Use `update-alias` to move the 'write' alias to the new index
3. Reindex all documents for the period since the remapping began
4. Use `update-alias` to move the 'read' alias to the new index


Commands in detail
------------------

To run any command, SSH onto the Elasticsearch machine, then:

    $ java -jar elasticsearch-remap-tool.jar [command with arguments]

### `remap`

    remap sourceIndex targetIndex [mappingsFile] [batchSize] [writeTimeOutInMilliseconds]

The `batchSize` parameter determines the number of documents updated at a time. It defaults to 500 and will not accept a value lower than 10.

The `writeTimeOutInMilliseconds` parameter determines the timeout for index actions against the new index. It defaults to 30000 (with a minimum value of 1000). There is no timeout applied to reads from the existing index.

You should run remaps in `screen` so that you can detach from the terminal if needed. Depending on the size of your index, it can take hours to complete.

### `update-alias`

    update-alias sourceIndex targetIndex alias


Potential pitfalls
------------------

Make sure that your source index, target index, and aliases all exist.

If you provide your own `mappings.json` be careful to make sure it is correct. You should probably never pass in a mappings file when remapping. Instead update the `default_mapping.json` file and merge it in before performing a remap. Elasticsearch will then apply this mapping automatically to any new index at creation time.

Lastly, make sure any alias used for insert operations only points to a single index at a time; Elasticsearch will not process insert operations against an alias with multiple indices.
