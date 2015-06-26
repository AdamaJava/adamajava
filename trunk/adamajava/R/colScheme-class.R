##############################################################################
#
#  Function: colScheme-class.R
#  Author:   John V Pearson
#  Created:  2011-10-27
#
#  This class is designed to hold a color scheme for use in plotting NGS
#  data from instances of block-class.  It must hold tuples of 
#  { number, color, label } where number is a value that appears in the
#  block-class@values data.frame, color is the color of the box to be
#  drawn when the number is seen, and label is the string description of
#  the number that would be shown in a plot legend.
#
#  Copyright 2011 The University of Queensland.
#
#  See the LICENSE file in this distribution for more details.
#
#  $Id: colScheme-class.R 4390 2011-11-30 23:08:42Z j.pearson $
#
##############################################################################


# "Cry havoc, and let slip the dogs of war ..."

### Define Class #####################################################

setClass("colScheme",
         representation(
             values     = "data.frame",
             created_on = "character",
             c_version  = "character"
             ),
         prototype=list(
             values     = data.frame(),
             colors     = c(),
             labels     = c(),
             created_on = date(),
             c_version  = grep( "^[[:digit:].]+$",
                                unlist(strsplit("$Revision: 4390 $","\\s")),
                                value=TRUE )
             )
        )


### Get Accessor methods #############################################

if (is.null(getGeneric("values")))
    setGeneric("values", function(object) standardGeneric("values"))

setMethod("values", "colScheme",
          function (object) object@values )


if (is.null(getGeneric("get.values")))
    setGeneric("get.values", function(object) standardGeneric("get.values"))

setMethod("get.values", "colScheme",
          function (object) row.names(object@values) )


if (is.null(getGeneric("get.labels")))
    setGeneric("get.labels", function(object) standardGeneric("get.labels"))

setMethod("get.labels", "colScheme",
          function (object) as.vector( object@values[,"labels"] ) )


if (is.null(getGeneric("get.colors")))
    setGeneric("get.colors", function(object) standardGeneric("get.colors"))

setMethod("get.colors", "colScheme",
          function (object) as.vector( object@values[,"colors"] ) )


if (is.null(getGeneric("values.count")))
    setGeneric("values.count", function(object) standardGeneric("values.count"))

setMethod("values.count", "colScheme",
          function (object) length(row.names(object@values)) )


if (is.null(getGeneric("created.on")))
    setGeneric("created.on", function(object) standardGeneric("created.on"))

setMethod("created.on", "colScheme",
          function (object) object@created_on )


if (is.null(getGeneric("code.version")))
    setGeneric("code.version", function(object) standardGeneric("code.version"))

setMethod("code.version", "colScheme",
          function (object) object@c_version )


### Set Accessor methods #############################################


### Processing methods ###############################################

if (is.null(getGeneric("summary")))
    setGeneric("summary", function(object,...) standardGeneric("summary"))

setMethod("summary", "colScheme",
    function (object) {
        cat( "S4 Object Class:     ", class(object), "\n" )
        cat( "Creation date:       ", created.on(object), "\n" )
        cat( "Code version:        ", code.version(object), "\n" )
    }
)


if (is.null(getGeneric("pick.label")))
    setGeneric("pick.label", function(object,...) standardGeneric("pick.label"))

setMethod("pick.label", "colScheme",
    function (object,value) {
        return( object@values[as.character(value),"labels"] )
    }
)


if (is.null(getGeneric("pick.color")))
    setGeneric("pick.color", function(object,...) standardGeneric("pick.color"))

setMethod("pick.color", "colScheme",
    function (object,value) {
        return( object@values[as.character(value),"colors"] )
    }
)



### Non-OO Helper Functions ##########################################

newColScheme <- function( values=c(), labels=c(), colors=c() ) {
    # Create and return colScheme object
    v <- data.frame( values, I(labels), I(colors), row.names=1 )
    names(v) <- c("labels","colors")
    cs <- new("colScheme", values=v)
    return( cs )
}
