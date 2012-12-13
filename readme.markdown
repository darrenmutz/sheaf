Sheaf is a static blogging engine that accepts html- or
markdown-formatted files as input.

# Usage

    Switches                     Default  Desc
    --------                     -------  ----
    -p, --no-publish, --publish  false    Publish an article
    -r, --no-revise, --revise    false    Revise an article
    -d, --no-delete, --delete    false    Delete an article
    -m, --month                           Month an article to revise was published in
    -y, --year                            Year an article to revise was published in
    -s, --slug                            Article slug, ex: my-article-title
    -t, --title                           The article's title
    -l, --link                            Title links externally link, ex: "http://www.noaa.gov"
    -a, --article                         File containing an html or markdown article, ex: path/to/article.html -or- path/to/another-article.md

Sheaf expects configuration to be located in a file called '.sheaf' in
your home directory. Here's an example;

    (def ^:dynamic *config* { :blog-title "A Decade Removed"
                              :blog-subtitle "As it was, so it shall be"
                              :base-url "example.org"
                              :atom-filename "index.xml"
                              :feed-id "http://mydomain.org/index.xml"
                              :author-name "Courier Botanical"
                              :author-uri "http://example.org/"
                              :sheaf-root "/path-to-datastore-home/sheaf"
                              :archive-dir "dir-to-store-article-metadata"
                              :template-file "template.html"
                              :input-article-selector [:article :> any-node]
                              :articles-selector [:section :article :body]
                              :article-selector [:article]
                              :index-articles-selector [:#articles]
                              :page-title-selector [:head :title]
                              :title-selector [:article :h2 :a]
                              :time-selector [:time]
                              :permalink-selector [:article :header :p :a]
                              :archives-selector [:section#archives]
                              :archive-list-selector [:.archive-list]
                              :doc-root "/path-to-output-htdocs"
                              :max-home-page-articles 20 })
