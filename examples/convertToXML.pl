#!/usr/bin/perl

use warnings;

# converts an ICICLE parse tree file to XML

# settings
$USE_FEATURES = 'true';

# arguments
($filename, $outfilename) = @ARGV;

if (@ARGV < 2)
	{
	print "Insufficient arguments\n";
	print <<USAGE;
Usage: perl convertToXML.pl inputfile outputfile

The output of this Perl script isn't directly loadable into
the parse tree application. This program produces an XML file
which maps sentences to sets of parse trees. The file format
for the Parse Tree Application is simply a set of parse trees.
So, to use it with PTA, copy and paste a set of trees from
the output of this program into its own XML file. 

USAGE

	exit;
	}

$omitted_feature_counts = {};

open FILE, $filename or die "Unable to open input file $filename\n";
$file = join '', <FILE>;
close FILE;

@sentenceGroups = split /(?:^|\n)Sentence: \d+ --\s*/, $file;

@sentenceGroups = grep { $_ !~ /^\s*$/s } @sentenceGroups;

print "Found " . scalar(@sentenceGroups) . " sentences\n";

open OUTFILE, ">$outfilename" or die "Unable to open output file $outfilename\n";
print OUTFILE "<sentences>\n";
foreach $sentence (@sentenceGroups)
	{
	$sentence =~ /^\s*(\S[^\n]*\S)(.*)$/s;
	$string = $1;
	$rest = $2;

	# escape quotes in the string
	$string =~ s/\"/\\\"/g;
	print OUTFILE "<sentence string = \"$string\">\n";

	undef $timedOut;
	undef @trees;
	if ($rest =~ /TIMED OUT/)
		{
		$timedOut = 'true';
#		print "\tTimed out\n";
		}
	elsif ($rest =~ /NO PARSE TREES FOUND/)
		{
#		print "\tNo parses\n";
		}
	else
		{
		@trees = split /-+Parse Tree: \d+-+/, $rest;

		foreach $tree (@trees)
			{
			# split each tree into the grouped lines
			@nodes = grep { $_ !~ /^[\s=\-]*$/ } split /\n\n/, $tree;

			@parseStack = ();
			foreach $node (@nodes)
				{
				# split it into lines
				@lines = grep { $_ !~ /^\s*$/ } split /\n/, $node;

				# figure out the level of embedding
				if ($lines[0] =~ /^(>*)([^>]*)\s*$/)
					{
					$level = length $1;

					$name = $2;
					chomp $name;

					$constituent = { 'name' => $name, 'features' => {}, 'level' => $level };

					# load the features
					if ($lines[-1] =~ /^(?:>*)\s*\((.*)\)\s*$/)
						{
						@features = split /\)\(/, $1;

						foreach $feature (@features)
							{
							if ($feature =~ /^(\S+)\s+(\S.*)$/)
								{
								$featureName = $1;
								$featureValue = $2;

								$constituent->{'features'}->{$featureName} = $featureValue;
								}
							else
								{
								die "Unable to parse feature\n";
								}
							}
						}
					else
						{
						print "No features? $lines[-1]\n";
						}
					
					# see what to do
					if (not defined $oldConstituent)
						{
						openConstituent($constituent);
						}
					elsif ($constituent->{'level'} == $oldConstituent->{'level'})
						{
						closeConstituent($oldConstituent);
						openConstituent($constituent, 'true');
						}
					elsif ($constituent->{'level'} == $oldConstituent->{'level'} + 1)
						{
						push @parseStack, $oldConstituent;
						$oldConstituent->{'non-terminal'} = 'true';
						openConstituent($constituent);
						}
					elsif ($constituent->{'level'} < $oldConstituent->{'level'})
						{
						closeConstituent($oldConstituent);

						while ($constituent->{'level'} < $oldConstituent->{'level'})
							{
							$oldConstituent = pop @parseStack;
							closeConstituent($oldConstituent);
							}
						openConstituent($constituent, 'true');
						}
					else
						{
						die "Level of embedding jumped too much\n";
						}
					
					$oldConstituent = $constituent;
					}
				else
					{
					die "Unable to find embedding at line $lines[0]\n";
					}
				}
			
			# close off everything left over
			closeConstituent($oldConstituent) if (defined $oldConstituent);
			while (@parseStack > 0)
				{
				$oldConstituent = pop @parseStack;
				closeConstituent($oldConstituent);
				}
			undef $oldConstituent;

			print OUTFILE "\n";
			}
		}
	print OUTFILE "</sentence>\n";
	}
print OUTFILE "</sentences>\n";
close OUTFILE;

print_omitted_feature_summary();

sub print_omitted_feature_summary
	{
	my @keys = sort keys %$omitted_feature_counts;
	
	return if (@keys == 0);
	
	print "Some features were omitted, due to names which aren't valid XML attribute names.\nSummary:\n";
	foreach $key (@keys)
		{
		print "\tAttribute $key omitted $omitted_feature_counts->{$key} times\n";
		}
	}

sub openConstituent
	{
	my ($constituent, $canAddSpace) = @_;

	# add a space in if we can *and* if this isn't a subtree that exclusively has a gap
	if ($canAddSpace and not defined $constituent->{'features'}->{'EMPTY'})
		{
		addSpace();
		}
	
	my @featureKeys = keys %{$constituent->{'features'}};
	my @selectedFeatureKeys = grep { $_ =~ /^[a-zA-Z]/ } @featureKeys;
	my @notselectedFeatureKeys = grep { $_ !~ /^[a-zA-Z]/ } @featureKeys;
	if (@notselectedFeatureKeys > 0)
		{
#		print "Some features omitted, due to non-XML compliant names in tag $constituent->{'name'}:\n";
		foreach $attribute (@notselectedFeatureKeys)
			{
			$omitted_feature_counts->{$attribute}++;
#			print "\t$attribute = $constituent->{'features'}->{$attribute}\n";
			}
		}


	# compute the string for features
	my $featureString = join(' ', map { "$_ = \"$constituent->{'features'}->{$_}\"" } @selectedFeatureKeys );
	$featureString = " $featureString" if ($featureString ne '');
	$featureString = '' if (not defined $USE_FEATURES);

	if ($constituent->{'name'} !~ /^\w+$/)
		{
		print OUTFILE "<constituent label=\"$constituent->{name}\"$featureString>";
		}
	else
		{
		print OUTFILE "<$constituent->{name}$featureString>";
		}
	}

sub addSpace
	{
	print OUTFILE " ";
	}

sub closeConstituent
	{
	my ($constituent) = @_;

	# terminal symbols must have some contents
	if (not defined $constituent->{'non-terminal'})
		{
		# terminal symbol with a lexical entry
		if (defined $constituent->{'features'}->{'INPUT'})
			{
			print OUTFILE lc $constituent->{'features'}->{'INPUT'};
			}
		# a terminal symbol that's a gap
		elsif (defined $constituent->{'features'}->{'EMPTY'} and $constituent->{'features'}->{'EMPTY'} eq '+')
			{
			# don't print anything
#			print OUTFILE "*gap*";
#			print OUTFILE "  ";
			}
		# if it's an unknown terminal type, display it
		else
			{
			print "Unknown terminal symbol type named $constituent->{name}\n";
			foreach $feature (keys %{$constituent->{'features'}})
				{
				print "\t$feature = $constituent->{'features'}->{$feature}\n";
				}
			}
		}

	if ($constituent->{'name'} !~ /^\w+$/)
		{
		print OUTFILE "</constituent>";
		}
	else
		{
		print OUTFILE "</" . $constituent->{'name'} . ">";
		}
	}
