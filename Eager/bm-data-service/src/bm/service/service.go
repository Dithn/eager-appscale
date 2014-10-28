package main

import (
	"fmt"
	"bm/db"
)

func main() {
	fsd, err := db.NewFSDatabase("/Users/hiranya/Projects/eager/impl/eager-appscale/Eager/appscale-benchmark-app")
	if err != nil {
		fmt.Println(err)
		return
	}
	result := fsd.Query(1005, []string{"com.google.appengine.api.datastore.DatastoreService#get()"})
	for k, v := range result {
		fmt.Println(k, "=>", v, len(v))
	}
}
