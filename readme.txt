# Sheaf

Sheaf is a static blogging engine.

## Usage

 Switches                     Default  Desc
 --------                     -------  ----
 -p, --no-publish, --publish  false    Publish an article                                     
 -r, --no-revise, --revise    false    Revise an article
 -m, --month                           Month an article to revise was published in
 -y, --year                            Year an article to revise was published in
 -d, --no-delete, --delete    false    Delete an article                                      
 -s, --slug                            Article slug, ex: my-article-title                     
 -l, --link                            Title is an offsite link, ex: "http://www.noaa.gov"    
 -h, --html                            File containing html article, ex: path/to/article.html 


Sheaf expects configuration to be located in a file called '.sheaf' in
your home directory. Here's an example;

(def ^:dynamic *config* { :blog-title "A Decade Removed"
                          :blog-subtitle "As it was, so it shall be"
                          :base-url "example.org"
                          :atom-filename "index.xml"
                          :uuid "unique-identifier"
                          :author-name "Courier Botanical"
                          :author-uri "http://example.org/"
                          :sheaf-root "/path-to-datastore-home/sheaf"
                          :archive-dir "dir-to-store-article-metadata"
                          :template-file "template.html"
                          :input-article-selector [:article :> any-node]
                          :articles-selector [:section :article :body]
                          :article-selector [:article]
                          :article-body-selector [:#articles :body]
                          :input-title-selector [:title :> text-node]
                          :index-articles-selector [:#articles]
                          :title-selector [:article :h2 :a]
                          :time-selector [:time]
                          :permalink-selector [:article :header :p :a]
                          :archives-selector [:section#archives]
                          :archive-list-selector [:.archive-list]
                          :doc-root "/path-to-output-htdocs"
                          :max-home-page-articles 20 })

## License

Copyright (c) 2012, Darren Mutz
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
  * Redistributions of source code must retain the above copyright
    notice, this list of conditions and the following disclaimer.
  * Redistributions in binary form must reproduce the above
    copyright notice, this list of conditions and the following
    disclaimer in the documentation and/or other materials provided
    with the distribution.
  * Neither the name of Darren Mutz nor the names of its
    contributors may be used to endorse or promote products derived
    from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
OF THE POSSIBILITY OF SUCH DAMAGE.
