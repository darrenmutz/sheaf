Sheaf is a static (or baked) blogging engine. This means the output of
the engine is a collection of static html that can be served rapidly
by a web server like Apache running on hardware with modest resources.
The primary tradeoff is that dynamic behaviors like comments aren't
supported.

# Features

* Publish, revise and delete markdown or HTML articles in the markup
  of your choice, as expressed by an HTML template you provide.
* Organize published articles hierarchically by year and month.
* Generate chronologically ordered archives for each month and year
  articles are published in.
* Generate a root object (index.html) containing recent articles in
  reverse chronological order.
* Generate a blog feed in the Atom Syndication Format.
* Perform typographic transformations to markdown articles (e.g.,
  curly quotes, long dashes and elipses).
* Optionally watch an article draft and publish revisions live for
  proofreading articles as they will appear to your readers.

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

# Caveats

Sheaf doesn't differentiate between drafts and final published
articles. To avoid a confusing experience for your readers it is
recommended that you publish to a non-public (or local) document root
while an article is being revised.

