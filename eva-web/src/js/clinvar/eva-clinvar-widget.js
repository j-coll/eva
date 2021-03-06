/*
 * Copyright (c) 2014 Francisco Salavert (SGL-CIPF)
 * Copyright (c) 2014 Alejandro Alemán (SGL-CIPF)
 * Copyright (c) 2014 Ignacio Medina (EBI-EMBL)
 * Copyright (c) 2014 Jag Kandasamy (EBI-EMBL)
 *
 * This file is part of EVA.
 *
 * EVA is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * EVA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with EVA. If not, see <http://www.gnu.org/licenses/>.
 */
function EvaClinVarWidget(args) {

    _.extend(this, Backbone.Events);

    this.id = Utils.genId("VariantWidget");

    //set default args
    this.target;
    this.width;
    this.height;
    this.autoRender = true;
    this.data = [];
    this.host;
    this.closable = true;
    this.filters = {
        segregation: true,
        maf: true,
        effect: true,
        region: true,
        gene: true
    };
    this.headerConfig;
    this.attributes = [];
    this.columns = [];
    this.samples = [];
    this.defaultToolConfig = {
        headerConfig: {
            baseCls: 'ocb-title-2'
        },
        genomeViewer: true,
        genotype: true,
    };
    this.tools = [];
    this.dataParser;
    this.responseParser;

    this.responseRoot = "response[0].result";
    this.responseTotal = "response[0].numResults";
    this.startParam = "skip";

    this.browserGridConfig = {
        title: 'ClinVar browser grid',
        border: false
    };
    this.toolPanelConfig = {
        title: 'ClinVar data',
        border: false
    };
    this.toolsConfig = {
        headerConfig: {
            baseCls: 'ocb-title-2'
        }
    };


    _.extend(this.filters, args.filters);
    _.extend(this.browserGridConfig, args.browserGridConfig);
    _.extend(this.defaultToolConfig, args.defaultToolConfig);

    delete args.filters;
    delete args.defaultToolConfig;

//set instantiation args, must be last
    _.extend(this, args);

    this.selectedToolDiv;

    this.rendered = false;
    if (this.autoRender) {
        this.render();
    }

}

EvaClinVarWidget.prototype = {
    render: function () {
        var _this = this;

        //HTML skel
        this.div = document.createElement('div');
        this.div.setAttribute('id', this.id);

        this.clinvarBrowserGridDiv = document.createElement('div');
        this.clinvarBrowserGridDiv.setAttribute('class', 'ocb-variant-widget-grid');
        this.div.appendChild(this.clinvarBrowserGridDiv);

        this.clinvarBrowserGrid = this._createClinVarBrowserGrid(this.clinvarBrowserGridDiv);

        this.tabPanelDiv = document.createElement('div');
        this.tabPanelDiv.setAttribute('class', 'ocb-variant-tab-panel');
        this.div.appendChild(this.tabPanelDiv);

        this.toolTabPanel = Ext.create("Ext.tab.Panel", {
            title: this.toolPanelConfig.title,
            border: this.toolPanelConfig.border,
            margin: '10 0 0 0',
            height:600,
            plain: true,
            animCollapse: false,
            header: this.toolPanelConfig.headerConfig,
            collapseDirection: Ext.Component.DIRECTION_BOTTOM,
            titleCollapse: true,
            overlapHeader: true,
            defaults: {
                hideMode: 'offsets',
                autoShow: true
            },
            listeners: {
                tabchange: function (tabPanel, newTab, oldTab, eOpts) {
                    _this.selectedToolDiv = newTab.contentEl;
                    if (_this.lastVariant) {
//                        _this.trigger('variant:change', {variant: _this.lastVariant, sender: _this});
                    }
                }
            }
        });

        var tabPanelItems = [];

        if (this.defaultToolConfig.assertion) {
            this.clinvarAssertionPanelDiv = document.createElement('div');
            this.clinvarAssertionPanelDiv.setAttribute('class', 'ocb-variant-stats-panel');
            this.clinvarAssertionPanel = this._createAssertionPanel(this.clinvarAssertionPanelDiv);
            tabPanelItems.push({
                title: 'Assertion',
//                border: 0,
                contentEl: this.clinvarAssertionPanelDiv
            });
        }
        for (var i = 0; i < this.tools.length; i++) {
            var tool = this.tools[i];
            var toolDiv = document.createElement('div');

            tool.tool.target = toolDiv;

            tabPanelItems.push({
                title: tool.title,
                contentEl: toolDiv
            });
        }

        this.toolTabPanel.add(tabPanelItems);

        this.rendered = true;
    },
    draw: function () {
        var _this = this;
        this.targetDiv = (this.target instanceof HTMLElement ) ? this.target : document.querySelector('#' + this.target);
        if (!this.targetDiv) {
            console.log('EVAVAriantWidget target not found');
            return;
        }
        this.targetDiv.appendChild(this.div);

        this.clinvarBrowserGrid.draw();

        this.toolTabPanel.render(this.tabPanelDiv);


        for (var i = 0; i < this.toolTabPanel.items.items.length; i++) {
            this.toolTabPanel.setActiveTab(i);
        }

        if (this.defaultToolConfig.assertion) {
            this.clinvarAssertionPanel.draw();
        }

        for (var i = 0; i < this.tools.length; i++) {
            var tool = this.tools[i];
            tool.tool.draw();
        }

        this.toolTabPanel.setActiveTab(0);
    },
    _createClinVarBrowserGrid: function (target) {
        var _this = this;

        var columns ={
            items:[
                {
                    text: "Accession",
//                    dataIndex: 'accession',
                    xtype: "templatecolumn",
                    tpl: '<tpl>{referenceClinVarAssertionAcc}</tpl>',
                    flex: 1
                },
                {
                    text: 'Description',
                    dataIndex: 'description',
                    flex: 0.8
                },
                {
                    text: "Trait",
                    dataIndex: 'trait',
                    flex: 2
                },
                {
                    text: 'Platform',
                    dataIndex: 'platform',
                    flex: 0.7
                },
                {
                    text: 'Technology',
                    dataIndex: 'technology',
                    flex: 0.7
                },
                {
                    text: "Gene",
//                    dataIndex: 'accession',
                    id:'clinvar-grid-gene-column',
                    xtype: "templatecolumn",
                    tpl: '<tpl></tpl>',
                    flex: 0.5
                },
                {
                    text: "Xrefs",
                    flex: 1,
                    textAlign: 'center',
                    columns: [
                        {
                            text: "ID",
                            dataIndex: "xref_id"

                        },
                        {
                            text: "Database",
                            dataIndex: "xref_db"
                        }
                    ]
                },
                {
                    text: "View",
                    id:'clinvar-grid-view-column',
                    xtype: "templatecolumn",
                    tpl: '<tpl></tpl>',
                    flex: 1.2
                }

            ],
            defaults: {
                textAlign: 'center',
                align:'left' ,
                sortable : false
            }
        } ;

        var attributes = [
            {name: 'referenceClinVarAssertionAcc', mapping: 'referenceClinVarAssertion.clinVarAccession.acc', type: 'string' },
            {name: 'clinVarAssertionAcc', mapping: 'clinVarAssertion[0].clinVarAccession.acc', type: 'string' },
            {name: 'description', mapping: 'referenceClinVarAssertion.clinicalSignificance.description', type: 'string' },
            {name: 'trait', mapping: 'clinVarAssertion[0].observedIn[0].traitSet.trait[0].name[0].elementValue.value', type: 'auto' },
            {name: 'platform', mapping: 'clinVarAssertion[0].observedIn[0].method[0].typePlatform', type: 'string' },
            {name: 'technology', mapping: 'clinVarAssertion[0].observedIn[0].method[0].description', type: 'string' },
            {name: 'xref_id', mapping: 'clinVarAssertion[0].observedIn[0].xref[0].id', type: 'string' },
            {name: 'xref_db', mapping: 'clinVarAssertion[0].observedIn[0].xref[0].db', type: 'string' }
        ];


        var clinvarBrowserGrid = new ClinvarBrowserGrid({
            title: this.browserGridConfig.title,
            target: target,
            data: this.data,
            border: this.browserGridConfig.border,
            dataParser: this.dataParser,
            responseRoot: this.responseRoot,
            responseTotal: this.responseTotal,
            responseParser: this.responseParser,
            startParam: this.startParam,
            attributes: attributes,
            columns:columns,
            samples: this.samples,
            headerConfig: this.headerConfig
        });
        return clinvarBrowserGrid;
    },
    _createAssertionPanel: function (target) {
        var _this = this;
        var assertionPanel = new ClinvarAssertionPanel({
            target: target,
            headerConfig: this.defaultToolConfig.headerConfig,
            handlers: {
                "load:finish": function (e) {
//                    _this.grid.setLoading(false);
                }
            }

        });

        this.clinvarBrowserGrid.on("clinvar:clear", function (e) {
            assertionPanel.clear(true);
        });

        this.clinvarBrowserGrid.on("clinvar:change", function (e) {
            if (target === _this.selectedToolDiv) {
                assertionPanel.load(e.args);
            }
        });

        return assertionPanel;
    },

    _createGenomeViewer: function (target) {
        var _this = this;


        var region = new Region({
            chromosome: "13",
            start: 32889611,
            end: 32889611
        });

        var genomeViewer = new GenomeViewer({
            sidePanel: false,
            target: target,
            border: false,
            resizable: true,
            width: this.width,
            region: region,
            trackListTitle: '',
            drawNavigationBar: true,
            drawKaryotypePanel: false,
            drawChromosomePanel: false,
            drawRegionOverviewPanel: true,
            overviewZoomMultiplier: 50,
            navigationBarConfig: {
                componentsConfig: {
                    restoreDefaultRegionButton: false,
                    regionHistoryButton: false,
                    speciesButton: false,
                    chromosomesButton: false,
                    karyotypeButton: false,
                    chromosomeButton: false,
                    regionButton: false,
//                    zoomControl: false,
                    windowSizeControl: false,
//                    positionControl: false,
//                    moveControl: false,
//                    autoheightButton: false,
//                    compactButton: false,
//                    searchControl: false
                }
            }
        });
        genomeViewer.setZoom(80);

        var renderer = new FeatureRenderer(FEATURE_TYPES.gene);
        renderer.on({
            'feature:click': function (event) {
                // feature click event example
                console.log(event)
            }
        });
        var geneOverview = new FeatureTrack({
//        title: 'Gene overview',
            minHistogramRegionSize: 20000000,
            maxLabelRegionSize: 10000000,
            height: 100,

            renderer: renderer,

            dataAdapter: new CellBaseAdapter({
                category: "genomic",
                subCategory: "region",
                resource: "gene",
                params: {
                    exclude: 'transcripts,chunkIds'
                },
                species: genomeViewer.species,
                cacheConfig: {
                    chunkSize: 100000
                }
            })
        });


        var sequence = new SequenceTrack({
//        title: 'Sequence',
            height: 30,
            visibleRegionSize: 200,

            renderer: new SequenceRenderer(),

            dataAdapter: new SequenceAdapter({
                category: "genomic",
                subCategory: "region",
                resource: "sequence",
                species: genomeViewer.species
            })
        });


        var gene = new GeneTrack({
            title: 'Gene',
            minHistogramRegionSize: 20000000,
            maxLabelRegionSize: 10000000,
            minTranscriptRegionSize: 200000,
            height: 60,

            renderer: new GeneRenderer(),

            dataAdapter: new CellBaseAdapter({
                category: "genomic",
                subCategory: "region",
                resource: "gene",
                species: genomeViewer.species,
                params: {
                    exclude: 'transcripts.tfbs,transcripts.xrefs,transcripts.exons.sequence'
                },
                cacheConfig: {
                    chunkSize: 100000
                }
            })
        });

        var snp = new FeatureTrack({
            title: 'SNP',
            featureType: 'SNP',
            minHistogramRegionSize: 10000,
            maxLabelRegionSize: 3000,
            height: 100,

            renderer: new FeatureRenderer(FEATURE_TYPES.snp),

            dataAdapter: new CellBaseAdapter({
                category: "genomic",
                subCategory: "region",
                resource: "snp",
                params: {
                    exclude: 'transcriptVariations,xrefs,samples'
                },
                species: genomeViewer.species,
                cacheConfig: {
                    chunkSize: 10000
                }
            })
        });

        genomeViewer.addOverviewTrack(geneOverview);
        genomeViewer.addTrack([sequence, gene, snp]);


        this.on("clinvar:change", function (e) {
            if (target === _this.selectedToolDiv) {
                var variant = e.variant;
                var region = new Region(variant);
                if (!_.isUndefined(genomeViewer)) {
                    genomeViewer.setRegion(region);
                }
            }
        });

        return genomeViewer;
    },
    retrieveData: function (baseUrl, filterParams) {
        this.clinvarBrowserGrid.loadUrl(baseUrl, filterParams);
    },
    setLoading: function (loading) {
        this.variantBrowserGrid.setLoading(loading);
    }
};
