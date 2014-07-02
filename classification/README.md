Step 1. Extract content items played
============================================================

```bash
hadoop jar $STREAMING -mapper kid_map.py -file kid_map.py -reducer kid_reduce.py -file kid_reduce.py -input clean -output kid
```
