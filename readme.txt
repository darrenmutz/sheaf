# Sheaf

Sheaf is a static blogging engine.

## Usage

 Switches                     Default  Desc
 --------                     -------  ----
 -p, --no-publish, --publish  false    Publish an article
 -r, --no-revise, --revise    false    Revise an article
 -d, --no-delete, --delete    false    Delete an article
 -t, --title                           Article title, ex: my-article-title
 -h, --html                            File containing html article, ex: path/to/article.html

Sheaf expects configuration to be located in a file called '.sheaf' in
your home directory. This file should contain the following settings;

(def ^:dynamic *config* { :sheaf-root "/path-to-datastore-home/sheaf"
                          :archive-dir "dir-to-store-archive-metadata"
                          :template-file "your-template.html"
                          :articles-selector [:section :article :body]
			  :article-selector [:article]
			  :index-articles-selector [:section]
			  :title-selector [:article :h2]
			  :time-selector [:time] 
                          :doc-root "/path-to-output-htdocs"
			  :uri-root "http://www.mydomain.com/"
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
