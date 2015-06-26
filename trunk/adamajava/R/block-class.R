##############################################################################
#
#  Function: block-class.R
#  Author:   John V Pearson
#  Created:  2011-10-14
#
#  This class is designed to produce a generic block plot where you pass in a
#  data frame of numbers and each non-zero number denotes the color of that
#  block (based on captive instance of class colScheme).
#
#  Copyright 2011-2014 The University of Queensland
#  Copyright 2011-2014 John Pearson (grendeloz@gmail.com)
#
#  See the LICENSE file in this distribution for more details.
#
#  $Id: block-class.R 7971 2014-03-18 08:27:28Z j.pearson $
#
##############################################################################


# "Cry havoc, and let slip the dogs of war ..."

### Define Class #####################################################

setClass("block",
         representation(
             datafile     = "character",
             description  = "character",
             values       = "data.frame",
             colScheme    = "colScheme",
             created_on   = "character",
             c_version    = "character"
             ),
         prototype=list(
             datafile     = "",
             description  = "",
             values       = data.frame(),
             colScheme    = new("colScheme"),
             created_on   = date(),
             c_version    = grep( "^[[:digit:].]+$",
                                  unlist(strsplit("$Revision: 7971 $","\\s")),
                                  value=TRUE )
             )
        )


### Get Accessor methods #############################################

if (is.null(getGeneric("datafile")))
    setGeneric("datafile", function(object) standardGeneric("datafile"))

setMethod("datafile", "block",
          function (object) object@datafile )


if (is.null(getGeneric("description")))
    setGeneric("description", function(object) standardGeneric("description"))

setMethod("description", "block",
          function (object) object@description )


if (is.null(getGeneric("colScheme")))
    setGeneric("colScheme", function(object) standardGeneric("colScheme"))

setMethod("colScheme", "block",
          function (object) object@colScheme )


if (is.null(getGeneric("created_on")))
    setGeneric("created_on", function(object) standardGeneric("created_on"))

setMethod("created_on", "block",
          function (object) object@created_on )


if (is.null(getGeneric("row_count")))
    setGeneric("row_count", function(object) standardGeneric("row_count"))

setMethod("row_count", "block",
          function (object) length(object@values[,1]) )


if (is.null(getGeneric("col_count")))
    setGeneric("col_count", function(object) standardGeneric("col_count"))

setMethod("col_count", "block",
          function (object) length(object@values[1,]) )


if (is.null(getGeneric("c_version")))
    setGeneric("c_version", function(object) standardGeneric("c_version"))

setMethod("c_version", "block",
          function (object) object@c_version )


### Set Accessor methods #############################################

if (is.null(getGeneric("datafile<-")))
    setGeneric("datafile<-",
               function(object, value) standardGeneric("datafile<-"))

setReplaceMethod("datafile", "block",
    function (object, value) {
        object@datafile <- value
        object
    }
)

if (is.null(getGeneric("description<-")))
    setGeneric("description<-",
               function(object, value) standardGeneric("description<-"))

setReplaceMethod("description", "block",
    function (object, value) {
        object@description <- value
        object
    }
)

if (is.null(getGeneric("values<-")))
    setGeneric("values<-",
               function(object, value) standardGeneric("values<-"))

setReplaceMethod("values", "block",
    function (object, value) {
        object@values <- value
        object
    }
)

if (is.null(getGeneric("colScheme<-")))
    setGeneric("colScheme<-",
               function(object, value) standardGeneric("colScheme<-"))

setReplaceMethod("colScheme", "block",
    function (object, value) {
        object@colScheme <- value
        object
    }
)


### Processing methods #############################################

if (is.null(getGeneric("row.value.counts")))
    setGeneric("row.value.counts",
               function(object) standardGeneric("row.value.counts"))

# For a numeric matrix, work out how many of the cells in each rows
# contain a digit (even 0).  This is useful for proportion plot where we
# need to work out what the total is so we can draw appropriately sized
# block for each value type.

setMethod("row.value.counts", "block",
    function (object) { 
        row.maxs <- c()
        for (i in 1:row_count(object)) {
            row.maxs[i] <- length( grep( "\\d+", object@values[i,], perl=TRUE ) )
        }
        return( row.maxs )
    }
)

if (is.null(getGeneric("col.value.counts")))
    setGeneric("col.value.counts",
               function(object) standardGeneric("col.value.counts"))

# For a numeric matrix, work out how many of the cells in each column
# contain a digit (even 0).  This is useful for proportion plot where we
# need to work out what the total is so we can draw appropriately sized
# block for each value type.

setMethod("col.value.counts", "block",
    function (object) { 
        col.maxs <- c()
        for (i in 1:col_count(object)) {
            col.maxs[i] <- length( grep( "\\d+", object@values[,i], perl=TRUE ) )
        }
        return( col.maxs )
    }
)



### Plotting methods from block-class ##############################

# A gutter is the blank space between adjoining boxes.  It is the
# same in the vertical and horizontal dimensions and is expressed as
# a percentage (0..1) of a box.width.  If you know the relative size 
# relationship between box and gutter and the column count and
# the overall plot width, you can calculate the true sizes of
# gutters and box. We also allow for 2*gutters on the 4 edges of the
# plot to give neater separation when a border is drawn.

if (is.null(getGeneric("qVarMatrixPlot")))
    setGeneric("qVarMatrixPlot",
               function(object,...) standardGeneric("qVarMatrixPlot"))

setMethod( "qVarMatrixPlot", "block",
           function ( object,
                      x.offset=0,
                      y.offset=0,
                      x.size=10,
                      y.size=2,
                      x.gutter.ratio=0.1,  # 10% of a box
                      y.gutter.ratio=0.1,  # 10% of a box
                      box.color.empty=rgb(240,240,240,maxColorValue=255),
                      border.draw=FALSE,
                      border.color="black",
                      background.color="white"
                      ) {

    # This plot takes a matrix of numbers and draws a colored box for
    # each value where the color is determined by the value.

    # gutter.ratio : gutter size as percentage of box width
    # border.draw : should a border be drawn around the block plot
    # border.color : color of the border if drawn
    # x.offset, y.offset : used to shift a block diagram within a plot().
    #     this is particularly useful if you are placing multiple block
    #     diagrams within a single plot().
    # box.color.empty : empty boxes are light gray by default
    # background.color : plot background is white by default

    # Gutters: N-1 gutters between the N columns or rows plus 2 extra
    # gutters at each edge - top, bottom, left, right.

    col.scheme     <- object@colScheme
    x.gutter.count <- col_count(object) - 1 + 4
    x.gutter       <- x.size / ( col_count(object) * (1/x.gutter.ratio) + x.gutter.count )
    y.gutter.count <- row_count(object) - 1 + 4
    y.gutter       <- y.size / ( row_count(object) * (1/y.gutter.ratio) + y.gutter.count )
    box.width      <- 1/x.gutter.ratio * x.gutter
    box.height     <- 1/y.gutter.ratio * y.gutter

    #cat( "x.size: ",x.size,"  x.offset: ",x.offset,
    #     "  y.size: ",y.size,"  y.offset: ",y.offset,"\n")
    #cat( "box.width: ",box.width,"  box.height: ",box.height,"\n")
    #cat( "x.gutter: ",x.gutter,"  y.gutter: ",y.gutter,"\n")

    # Draw border if requested
    if (border.draw)
        polygon( c(x.offset, x.offset, x.offset+x.size, x.offset+x.size),
                 c(y.offset, y.offset+y.size, y.offset+y.size, y.offset),
                 border=border.color, col=background.color )
                    
    for (myx in 1:col_count(object)) {
        for (myy in 1:row_count(object)) {

            # Calculate bounds of this box
            x.left   <- x.offset + 2*x.gutter + (myx-1) * (x.gutter + box.width)
            x.right  <- x.left + box.width
            y.top    <- y.size + y.offset - 2*y.gutter - (myy-1) * (y.gutter + box.height)
            y.bottom <- y.top - box.height

            # Setup the color for this box
            this.value <- object@values[myy,myx]
            box.color <- box.color.empty
            if (! is.na(this.value))
                box.color <- pick.color( col.scheme, this.value )

            #cat( myx, myy, this.value, box.color, x.left, x.right, y.top, y.bottom, "\n" )

            # Draw the box using polygon()
            polygon( c( x.left, x.left, x.right, x.right ),
                     c( y.top, y.bottom, y.bottom, y.top ),
                     border=NA, col=box.color )
        }
    }
})


if (is.null(getGeneric("qFreqHorizPlot")))
    setGeneric("qFreqHorizPlot",
               function(object,...) standardGeneric("qFreqHorizPlot"))

setMethod( "qFreqHorizPlot", "block",
           function ( object,
                      x.offset=0,
                      y.offset=0,
                      x.size=10,
                      y.size=2,
                      mode=1,
                      gutter.ratio=0.1,  # 10% of a box
                      box.color.default=rgb(240,240,240,maxColorValue=255),
                      border.draw=FALSE,
                      border.color="black"
                      ) {

    # This plot takes a matrix of numbers and draws a series of horizontal
    # bars representing the frequency of each number in each row.

    col.scheme     <- object@colScheme
    y.gutter.count <- row_count(object) - 1 + 4
    y.gutter       <- y.size / ( row_count(object) * (1/gutter.ratio) + y.gutter.count )
    box.height     <- 1/gutter.ratio * y.gutter
    x.gutter.count <- col_count(object) - 1 + 4
    x.gutter       <- y.gutter

    # Draw border if requested
    if (border.draw)
        polygon( c(x.offset, x.offset, x.offset+x.size, x.offset+x.size),
                 c(y.offset, y.offset+y.size, y.offset+y.size, y.offset),
                 border=border.color )
                    
    # colScheme object drives values tallied and colors used
    my.vals <- as.numeric( get.values(col.scheme) )
    my.cols <- get.colors(col.scheme)

    for (i in 1:row_count(object)) {

        # The number of gutters in each row differs depending on how
        # many different values are in the row (i.e. how many boxes we
        # need to draw) so we need to do some work to see how many
        # value types are in the current row

        value.count      <- length(grep("\\d+",object@values[i,],value=TRUE))
        value.type.count <- length(unique(grep("\\d+",object@values[i,],value=TRUE)))

        # Initialise plotting params for this row
        x.span  <- x.size - (4 + value.type.count - 1) * x.gutter
        x.step  <- x.span / value.count
        x.left  <- x.offset + x.gutter
        x.right <- x.left

        #cat( i, "x.step:", x.step, " value.count:", value.count,
        #     " value.type.count:", value.type.count,
        #     " x.gutter:", x.gutter,
        #     " x.gutter.count:", x.gutter.count, "\n" );

        for (j in my.vals) {
            # How many of this type of value are in the row?
            count <- length(grep( j, object@values[i,]))

            if (count != 0) {
                # Calculate bounds of this box from right edge of previous box
                x.left   <- x.right + x.gutter
                x.right  <- x.left + count * x.step
                y.top    <- y.size + y.offset - 2*y.gutter -
                            ((i-1)* (box.height+y.gutter))
                y.bottom <- y.top - box.height

                #cat( row.names(object@values)[i], i, j,
                #     count, cumul, row.maxs[i], my.cols[j],
                #     x.left, x.right, y.top, y.bottom, "\n" )

                # Draw the box using polygon()
                polygon( c( x.left, x.left, x.right, x.right ),
                         c( y.top, y.bottom, y.bottom, y.top ),
                         border=NA, col=my.cols[j] )

            }
        }
    }
})


if (is.null(getGeneric("qFreqVertPlot")))
    setGeneric("qFreqVertPlot",
               function(object,...) standardGeneric("qFreqVertPlot"))

setMethod( "qFreqVertPlot", "block",
           function ( object,
                      x.offset=0,
                      y.offset=0,
                      x.size=10,
                      y.size=2,
                      mode=1,
                      gutter.ratio=0.1,  # 10% of a box
                      box.color.default=rgb(240,240,240,maxColorValue=255),
                      border.draw=FALSE,
                      border.color="black"
                      ) {

    # This plot takes a matrix of numbers and draws a series of vertical
    # bars representing the frequency of each number in each column.

    # mode=1: draw proportional blocks
    # mode=2: draw absolute count blocks

    col.scheme     <- object@colScheme
    x.gutter.count <- col_count(object) - 1 + 4
    x.gutter       <- x.size / ( col_count(object) * (1/gutter.ratio) + x.gutter.count )
    box.width      <- 1/gutter.ratio * x.gutter
    y.gutter.count <- row_count(object) - 1 + 4
    y.gutter       <- y.size / ( row_count(object) * (1/gutter.ratio) + y.gutter.count )

    # Draw border if requested
    if (border.draw)
        polygon( c(x.offset, x.offset, x.offset+x.size, x.offset+x.size),
                 c(y.offset, y.offset+y.size, y.offset+y.size, y.offset),
                 border=border.color )
                    
    # colScheme object drives values tallied and colors used
    my.vals <- as.numeric( get.values(col.scheme) )
    my.cols <- get.colors(col.scheme)

    # In mode 2 we will draw blocks that relate to the number of values
    # so in order to scale the plot, we need to know the single largest
    # number of values in any column and scale all others to this.

    if (mode == 2) {
        column.counts <- c()
        for (i in 1:col_count(object)) {
            column.counts[i] <- length(grep("\\d+",object@values[,i],value=TRUE))
        }
        max.column.count <- max( column.counts )
    }
    #cat( "max.column.count:",max.column.count,"\n")

    for (i in 1:col_count(object)) {

        # The number of gutters in each row differs depending on how
        # many different values are in the row (i.e. how many boxes we
        # need to draw) so we need to do some work to see how many
        # value types are in the current row

        value.count      <- length(grep("\\d+",object@values[,i],value=TRUE))
        value.type.count <- length(unique(grep("\\d+",object@values[,i],value=TRUE)))

        # Initialise plotting params for this column.  For mode=1 we
        # plot from the top down and for mode=2, from the bottom up
        y.span   <- y.size - (4 + value.type.count - 1) * y.gutter
        if (mode==1) {
            y.step   <- y.span / value.count
            y.top    <- y.size + y.offset - y.gutter
            y.bottom <- y.top
        }
        else if (mode==2) {
            y.step   <- y.span / max.column.count
            y.bottom <- y.offset + y.gutter
            y.top    <- y.bottom
        }

        #cat( "column ",i," starting vals -",
        #     "y.span:",y.span,
        #     "y.step:",y.step,
        #     "y.bottom:",y.bottom,
        #     "y.top:",y.top,"\n")

        for (j in my.vals) {
            # How many of this type of value are in the column?
            count <- length(grep( j, object@values[,i]))

            if (count != 0) {
                # Calculate bounds of this box from bottom edge of previous box
                if (mode == 1) {
                    y.top    <- y.bottom - y.gutter
                    y.bottom <- y.top - count * y.step
                }
                else if (mode == 2) {
                    y.bottom <- y.top + y.gutter
                    y.top    <- y.bottom + count * y.step
                }
                x.left   <- x.offset + 2*x.gutter + ((i-1)*(box.width+x.gutter))
                x.right  <- x.left + box.width

                # Draw the box using polygon()
                polygon( c( x.left, x.left, x.right, x.right ),
                         c( y.top, y.bottom, y.bottom, y.top ),
                         border=NA, col=my.cols[j] )
            }
        }
    }
})



if (is.null(getGeneric("diagram")))
    setGeneric("diagram", function(object,...) standardGeneric("diagram"))

setMethod( "diagram", "block",
           function ( object,
                      plot.type=1,
                      colors=c( "red", "orange", "yellow", "pink", 
                                "lightgreen", "green", "darkgreen",
                                "orange", "violet" ),
                      default.box.color=rgb(240,240,240,maxColorValue=255),
                      x.offset=0,
                      y.offset=0,
                      x.size=10,
                      y.size=2,
                      aspect.ratio=2,
                      label.space=0,
                      gutter.ratio=0.1,  # 10% of a box
                      border.draw=FALSE,
                      border.color="black"
                      ) {

    # Note that 255,255,255 is white and 0,0,0 is black in rgb()

    # aspect.ratio : ratio of height over width for blocks, default is 2
    # gutter.ratio : gutter size as percentage of box width
    # border.draw : should a border be drawn around the block plot
    # border.color : color of the border if drawn
    # x.offset, y.offset : used to shift a block diagram within a plot().
    #     this is particularly useful if you are placing multiple block
    #     diagrams within a single plot().
    # default.box.color : empty boxes are light gray by default

    # plot.type is used to choose between the following plot types:
    # Horizontal plots for above/below of variant matrix:
    # 2. Proportion plot - total each column and consider the value of each
    #    data item in the values data.frame to be the proportion of the
    #    total in that category and draw an appropriately vertically-scaled
    #    colored box.
    # 3. Bar plot - each value in the values data.frame determines
    #    the relative height of the colored box to be drawn.
    # Vertical plots for beside variant matrix:
    # 4. Proportion plot - as for 2 but with bars drawn horizontally
    # 5. Bar plot - as for 2 but with bars drawn horizontally

    # One of the tricks here is that we don't want any gutters except on
    # the 4 edges of the plot.  For plots 2,3 we don't use box.height
    # because the proportion of the plot will set it and for plots 4,5
    # this holds for box.width instead.
    
    x.gutter   <- x.size / (col_count(object) * (1/gutter.ratio) + 4 )
    box.width  <- 1/gutter.ratio * x.gutter
    x.span     <- x.size - (4 + col_count(object)) * x.gutter

    y.gutter   <- y.size / (row_count(object) * (1/gutter.ratio) + 4 )
    box.height <- 1/gutter.ratio * y.gutter
    y.span     <- y.size - (4 + row_count(object)) * y.gutter

    # Sum the rows and columns and work out the biggest one in each
    # category so we can scale the boxes in plot types 3 and 5.
    col.sums <- apply(object@values,2,sum,na.rm=TRUE)
    col.max  <- max( col.sums )
    y.step   <- y.span / col.max
    row.sums <- apply(object@values,1,sum,na.rm=TRUE)
    row.max  <- max( row.sums )
    x.step   <- x.span / row.max

    # Draw border if requested
    if (border.draw)
        polygon( c(x.offset, x.offset, x.offset+x.size, x.offset+x.size),
                 c(y.offset, y.offset+y.size, y.offset+y.size, y.offset),
                 border=border.color )

    if (plot.type == 2) {

        for (myx in 1:col_count(object)) {
            this.total <- col.sums[myx]
            this.cumul <- 0
            for (myy in 1:row_count(object)) {

                # We do nothing if this item is NA, else plot
                this.value <- object@values[myy,myx]
                if (! is.na(this.value)) {
                    box.color <- my.pickColor( myy, colors )

                    # Calculate bounds of this box
                    x.left   <- x.offset + 2*x.gutter + (myx-1) * box.width
                    x.right  <- x.left + box.width
                    y.bottom <- 2*y.gutter + this.cumul / this.total * y.span
                    y.top    <- y.bottom + this.value / this.total * y.span

                    this.cumul <- this.cumul + this.value

                    #cat( myx, myy, this.value, this.cumul, this.total,
                    #     x.left, x.right, y.top, y.bottom, "\n" )

                    # Draw the box using polygon()
                    polygon( c( x.left, x.left, x.right, x.right ),
                             c( y.top, y.bottom, y.bottom, y.top ),
                             border=NA, col=box.color )
                }
            }
        }
    }

    else if (plot.type == 3) {

        # Draw the boxes for each column
        for (myx in 1:col_count(object)) {
            this.total <- col.sums[myx]
            this.cumul <- 0
            for (myy in 1:row_count(object)) {

                # We do nothing if this item is NA, else plot
                this.value <- object@values[myy,myx]
                if (! is.na(this.value)) {
                    box.color <- my.pickColor( myy, colors )

                    # Calculate bounds of this box
                    x.left   <- x.offset + 2*x.gutter + (myx-1) * box.width
                    x.right  <- x.left + box.width
                    y.bottom <- y.offset + 2*y.gutter + this.cumul * y.step
                    y.top    <- y.bottom + this.value * y.step

                    this.cumul <- this.cumul + this.value

                    #cat( myx, myy, this.value, this.cumul, this.total,
                    #     x.left, x.right, y.top, y.bottom, "\n" )

                    # Draw the box using polygon()
                    polygon( c( x.left, x.left, x.right, x.right ),
                             c( y.top, y.bottom, y.bottom, y.top ),
                             border=NA, col=box.color )
                }
            }
        }
    }

    else if (plot.type == 4) {

        for (myy in 1:row_count(object)) {
            this.total <- row.sums[myy]
            this.cumul <- 0
            for (myx in 1:col_count(object)) {

                # We do nothing if this item is NA, else plot
                this.value <- object@values[myy,myx]
                if (! is.na(this.value)) {
                    box.color <- my.pickColor( myx, colors )

                    # Calculate bounds of this box
                    y.top    <- y.size - 2*y.gutter - (myy-1) * box.height
                    y.bottom <- y.top - box.height
                    x.left   <- 2*x.gutter + this.cumul / this.total * x.span
                    x.right  <- x.left + this.value / this.total * x.span

                    this.cumul <- this.cumul + this.value

                    #cat( myx, myy, this.value, this.cumul, this.total,
                    #     x.left, x.right, y.top, y.bottom, "\n" )

                    # Draw the box using polygon()
                    polygon( c( x.left, x.left, x.right, x.right ),
                             c( y.top, y.bottom, y.bottom, y.top ),
                             border=NA, col=box.color )
                }
            }
        }
    }
    
    else if (plot.type == 5) {

        # Same plot as 3 but drawn with horizontal bars. Typically sits
        # beside a variant matrix.

        for (myy in 1:row_count(object)) {
            this.total <- row.sums[myy]
            this.cumul <- 0
            for (myx in 1:col_count(object)) {

                # We do nothing if this item is NA, else plot
                this.value <- object@values[myy,myx]
                if (! is.na(this.value)) {
                    box.color <- my.pickColor( myx, colors )

                    # Calculate bounds of this box
                    y.top    <- y.size - 2*y.gutter - (myy-1) * box.height
                    y.bottom <- y.top - box.height
                    x.right  <- 2*x.gutter + (row.max -this.total + this.cumul) * x.step
                    x.left   <- x.right + this.value * x.step

                    # Calculate bounds of this box
                    #y.top    <- y.size - 2*y.gutter - (myy-1) * box.height
                    #y.bottom <- y.top - box.height
                    #x.right  <- 2*x.gutter + this.cumul * x.step
                    #x.left   <- x.right + this.value * x.step


                    this.cumul <- this.cumul + this.value

                    #cat( myx, myy, this.value, this.cumul, this.total,
                    #     x.left, x.right, y.top, y.bottom, "\n" )

                    # Draw the box using polygon()
                    polygon( c( x.left, x.left, x.right, x.right ),
                             c( y.top, y.bottom, y.bottom, y.top ),
                             border=NA, col=box.color )
                }
            }
        }
    }


    else {
        cat( "plot.type", plot.type, "is not currently implemented!\n")
    }

})


### Processing methods ###############################################

if (is.null(getGeneric("summary")))
    setGeneric("summary", function(object,...) standardGeneric("summary"))

setMethod("summary", "block",
    function (object) {
        cat( "S4 Object Class:     ", class(object), "\n" )
        cat( "Source file:         ", datafile(object), "\n" )
        cat( "Description:         ", description(object), "\n" )
        cat( "Column count:        ", col_count(object), "\n" )
        cat( "Row count:           ", row_count(object), "\n" )
        cat( "Creation date:       ", created_on(object), "\n" )
        cat( "Code version:        ", c_version(object), "\n" )
    }
)


### Non-OO Helper Functions ##########################################


my.pickColor <- function( this.value, colors ) {
    if (this.value == length(colors)) {
       this.color <- colors[ length(colors) ]
    }
    else {
       this.color <- colors[ this.value %% length(colors) ]
    }
    #cat( this.value, this.color, "\n" )
    return( this.color )
}


rowLabels <- function( x.min, x.max, y.min, y.max, names ) {

    # Calculate the amount of space available for each label and the
    # width and height of a line of text and if height or width are
    # greater than the space available then scale the text using cex.

    y.step  <- (y.max-y.min) / length(names)
    x.avail <- (x.max-x.min)
    max.text.width  <- max( strwidth( names ) )
    max.text.height <- max( strheight( names ) )
    my.cex.width  <- 1.0
    my.cex.height <- 1.0

    if (max.text.width > x.avail) {
        my.cex.width <- x.avail / max.text.width * 0.7
    }
    if (max.text.height > y.step) {
        my.cex.height <- y.step / max.text.height * 0.7
    }
    my.cex <- min( my.cex.width, my.cex.height, 1.0 )

    #cat( "rowLabels:",
    #     "max.text.width:",  max.text.width,
    #     "max.text.height:", max.text.height,
    #     "y.step:",  y.step,
    #     "x.avail:", x.avail,
    #     "my.cex.width:",  my.cex.width,
    #     "my.cex.height:", my.cex.height,
    #     "my.cex:", my.cex, "\n" )
    for (i in 1:length(names)) {
       myy <- y.max - (i-0.5) * y.step
       #cat( myy, names[i], "\n" )
       text( c(x.min), c(myy), names[i], pos=4, cex=my.cex )
    }
}


colLabels <- function( x.min, x.max, y.min, y.max, names ) {

    # Calculate the amount of space available for each label and the
    # width and height of a line of text and if height or width are
    # greater than the space available then scale the text using cex.

    x.step  <- (x.max-x.min) / length(names)
    y.avail <- (y.max-y.min)
    max.text.width  <- max( strwidth( names ) )
    max.text.height <- max( strheight( names ) )
    my.cex.width  <- 1.0
    my.cex.height <- 1.0

    if (max.text.width > y.avail) {
        my.cex.width <- y.avail / max.text.width * 0.7
    }
    if (max.text.height > x.step) {
        my.cex.height <- x.step / max.text.height * 0.7
    }
    my.cex <- min( my.cex.width, my.cex.height, 1.0 )

    #cat( "colLabels:",
    #     "max.text.width:",  max.text.width,
    #     "max.text.height:", max.text.height,
    #     "x.step:",  x.step,
    #     "y.avail:", y.avail,
    #     "my.cex.width:",  my.cex.width,
    #     "my.cex.height:", my.cex.height,
    #     "my.cex:", my.cex, "\n" )
    for (i in 1:length(names)) {
       myx <- x.min + (i+1) * x.step
       #cat( i, x.step, myx, y.max, names[i], "\n" )
       text( c(myx), c(y.max), names[i], pos=2, srt=90, cex=my.cex )
    }
}


# It'd be nice to use some extra font types in rowLabels and colLabels but
# I can't get the vfont stuff to work so far in my calls to text()
# vfont=c("sans","italic") )
# vfont=c("HersheySans","italic") )


loadBlock <- function( datafile, col.scheme, filedesc="" ) {

    # datafile       = "character",
    # description    = "character",
    # values         = "data.frame",
    # created_on     = "character",
    # c_version      = "character"

    cat( "loadBlock: datafile - ",datafile,"\n")

    tmpBlock <- read.csv( file=datafile, row.names=1 )

    # Create and return block object
    
    bl <- new("block", datafile=datafile,
                       description=filedesc,
                       values=tmpBlock,
                       colScheme=col.scheme )
    return( bl )
}
